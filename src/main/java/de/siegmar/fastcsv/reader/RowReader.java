package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.Reader;

/*
 * This class contains ugly, performance optimized code - be warned!
 */
final class RowReader {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private static final int LAST_CHAR_WAS_CR = 16;
    private static final int STATUS_NEW_FIELD = 8;
    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_COLUMN = 2;
    private static final int STATUS_DATA_COLUMN = 1;
    private static final int STATUS_RESET = 0;

    private final Buffer buffer;
    private final char fsep;
    private final char qChar;

    private int status;

    RowReader(final Reader reader, final char fieldSeparator, final char quoteCharacter) {
        buffer = new Buffer(reader);
        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
    }

    /**
     * Reads and parses data (one logical row that might span multiple lines) from reader into
     * {@link RowHandler}.
     *
     * @param rowHandler the rowHandler to handle the fetched data
     * @return {@code true} if end of stream reached
     * @throws IOException if a read error occurs
     */
    boolean fetchAndRead(final RowHandler rowHandler) throws IOException {
        do {
            if (buffer.len == buffer.pos) {
                // cursor reached current EOD -- need to fetch
                if (buffer.fetchData()) {
                    // reached end of stream
                    if (buffer.begin < buffer.pos) {
                        rowHandler.add(materialize(buffer.buf, buffer.begin,
                            buffer.pos - buffer.begin, status, qChar));
                    } else if ((status & STATUS_NEW_FIELD) != 0) {
                        rowHandler.add("");
                    }
                    return true;
                }
            }
        } while (consume(rowHandler));

        return false;
    }

    /**
     * Parses data from the current buffer into {@link RowHandler}.
     *
     * @param rowHandler the rowHandler to handle the fetched data
     * @return {@code true}, if more data is needed to complete current row
     */
    boolean consume(final RowHandler rowHandler) {
        final char[] lBuf = buffer.buf;
        final int lLen = buffer.len;

        int lPos = buffer.pos;
        int lBegin = buffer.begin;
        int lStatus = status;
        boolean moreDataNeeded = true;

        OUTER: {
            mode_check: do {
                if ((lStatus & STATUS_QUOTED_MODE) != 0) {
                    // we're in quotes
                    while (lPos < lLen) {
                        final char c = lBuf[lPos++];

                        if (c == qChar) {
                            lStatus &= ~STATUS_QUOTED_MODE;
                            continue mode_check;
                        } else if (c == CR) {
                            lStatus |= LAST_CHAR_WAS_CR;
                            rowHandler.incLines();
                        } else if (c == LF) {
                            if ((lStatus & LAST_CHAR_WAS_CR) == 0) {
                                rowHandler.incLines();
                            } else {
                                lStatus &= ~LAST_CHAR_WAS_CR;
                            }
                        } else {
                            // fast forward
                            while (lPos < lLen) {
                                final char lookAhead = lBuf[lPos++];
                                if (lookAhead == qChar || lookAhead == LF || lookAhead == CR) {
                                    lPos--;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    // we're not in quotes
                    while (lPos < lLen) {
                        final char c = lBuf[lPos++];

                        if (c == fsep) {
                            rowHandler.add(materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus,
                                qChar));
                            lStatus = STATUS_NEW_FIELD;
                            lBegin = lPos;
                        } else if (c == CR) {
                            rowHandler.add(materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus,
                                qChar));
                            status = LAST_CHAR_WAS_CR;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        } else if (c == LF) {
                            if ((lStatus & LAST_CHAR_WAS_CR) == 0) {
                                rowHandler.add(materialize(lBuf, lBegin, lPos - lBegin - 1,
                                    lStatus, qChar));
                                status = STATUS_RESET;
                                lBegin = lPos;
                                moreDataNeeded = false;
                                break OUTER;
                            }

                            lStatus = STATUS_RESET;
                            lBegin = lPos;
                        } else if (c == qChar && (lStatus & STATUS_DATA_COLUMN) == 0) {
                            // quote and not in data-only mode
                            lStatus = STATUS_QUOTED_COLUMN | STATUS_QUOTED_MODE;
                            continue mode_check;
                        } else {
                            if ((lStatus & STATUS_QUOTED_COLUMN) == 0) {
                                // normal unquoted data
                                lStatus = STATUS_DATA_COLUMN;

                                // fast forward
                                while (lPos < lLen) {
                                    final char lookAhead = lBuf[lPos++];
                                    if (lookAhead == fsep || lookAhead == LF || lookAhead == CR) {
                                        lPos--;
                                        break;
                                    }
                                }
                            } else {
                                // field data after closing quote
                            }
                        }
                    }
                }
            } while (lPos < lLen);

            status = lStatus;
        }

        buffer.pos = lPos;
        buffer.begin = lBegin;

        return moreDataNeeded;
    }

    private static String materialize(final char[] lBuf,
                                      final int lBegin, final int lPos, final int lStatus,
                                      final char quoteCharacter) {
        if ((lStatus & STATUS_QUOTED_COLUMN) == 0) {
            // column without quotes
            return new String(lBuf, lBegin, lPos);
        }

        // column with quotes
        final int shift = cleanDelimiters(lBuf, lBegin + 1, lBegin + lPos,
            quoteCharacter);
        return new String(lBuf, lBegin + 1, lPos - 1 - shift);
    }

    private static int cleanDelimiters(final char[] buf, final int begin, final int pos,
                                       final char quoteCharacter) {
        int shift = 0;
        boolean escape = false;
        for (int i = begin; i < pos; i++) {
            final char c = buf[i];

            if (c == quoteCharacter) {
                if (!escape) {
                    shift++;
                    escape = true;
                    continue;
                } else {
                    escape = false;
                }
            }

            if (shift > 0) {
                buf[i - shift] = c;
            }
        }

        return shift;
    }

    @SuppressWarnings("checkstyle:visibilitymodifier")
    private static class Buffer {
        private static final int READ_SIZE = 8192;
        private static final int BUFFER_SIZE = READ_SIZE;
        private static final int MAX_BUFFER_SIZE = 8 * 1024 * 1024;

        char[] buf = new char[BUFFER_SIZE];
        int len;
        int begin;
        int pos;

        private final Reader reader;

        Buffer(final Reader reader) {
            this.reader = reader;
        }

        /**
         * Reads data from the underlying reader and manages the local buffer.
         *
         * @return {@code true}, if EOD reached.
         * @throws IOException if a read error occurs
         */
        private boolean fetchData() throws IOException {
            if (begin < pos) {
                // we have data that can be relocated

                if (READ_SIZE > buf.length - pos) {
                    // need to relocate data in buffer -- not enough capacity left

                    final int lenToCopy = pos - begin;
                    if (READ_SIZE > buf.length - lenToCopy) {
                        // need to relocate data in new, larger buffer
                        buf = extendAndRelocate(buf, begin);
                    } else {
                        // relocate data in existing buffer
                        System.arraycopy(buf, begin, buf, 0, lenToCopy);
                    }
                    pos -= begin;
                    begin = 0;
                }
            } else {
                // all data was consumed -- nothing to relocate
                pos = begin = 0;
            }

            final int cnt = reader.read(buf, pos, READ_SIZE);
            if (cnt == -1) {
                return true;
            }
            len = pos + cnt;
            return false;
        }

        private static char[] extendAndRelocate(final char[] buf, final int begin) {
            final int newBufferSize = buf.length * 2;
            if (newBufferSize > MAX_BUFFER_SIZE) {
                throw new IllegalStateException("Maximum buffer size "
                    + MAX_BUFFER_SIZE + " is not enough to read data");
            }
            final char[] newBuf = new char[newBufferSize];
            System.arraycopy(buf, begin, newBuf, 0, buf.length - begin);
            return newBuf;
        }

    }

}
