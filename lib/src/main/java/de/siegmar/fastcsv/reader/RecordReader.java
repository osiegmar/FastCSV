package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/*
 * This class contains ugly, performance optimized code - be warned!
 */
@SuppressWarnings({
    "checkstyle:CyclomaticComplexity",
    "checkstyle:ExecutableStatementCount",
    "checkstyle:InnerAssignment",
    "checkstyle:JavaNCSS",
    "checkstyle:NestedIfDepth",
    "PMD.UnusedAssignment"
})
final class RecordReader implements Closeable {

    private static final int STATUS_LAST_CHAR_WAS_CR = 32;
    private static final int STATUS_COMMENTED_RECORD = 16;
    private static final int STATUS_NEW_FIELD = 8;
    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_FIELD = 2;
    private static final int STATUS_DATA_FIELD = 1;
    private static final int STATUS_RESET = 0;

    private final RecordHandler recordHandler;
    private final CsvBuffer csvBuffer;
    private final char fsep;
    private final char qChar;
    private final CommentStrategy cStrat;
    private final char cChar;

    private int status;
    private boolean finished;

    RecordReader(final RecordHandler recordHandler, final Reader reader, final char fieldSeparator,
                 final char quoteCharacter, final CommentStrategy commentStrategy, final char commentCharacter) {
        this.recordHandler = recordHandler;
        csvBuffer = new CsvBuffer(reader);
        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
    }

    RecordReader(final RecordHandler recordHandler, final String data, final char fieldSeparator,
                 final char quoteCharacter, final CommentStrategy commentStrategy, final char commentCharacter) {
        this.recordHandler = recordHandler;
        csvBuffer = new CsvBuffer(data);
        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
    }

    boolean fetchAndRead() throws IOException {
        if (finished) {
            return false;
        }

        boolean fetched = true;
        do {
            if (csvBuffer.len == csvBuffer.pos) {
                // cursor reached current EOD -- need to fetch
                if (csvBuffer.fetchData()) {
                    finished = true;

                    // reached end of stream
                    if (csvBuffer.begin < csvBuffer.pos || recordHandler.isCommentMode()) {
                        materialize(csvBuffer.buf, csvBuffer.begin,
                            csvBuffer.pos - csvBuffer.begin, status, qChar);
                    } else if ((status & STATUS_NEW_FIELD) != 0) {
                        recordHandler.add("", false);
                    } else {
                        fetched = false;
                    }

                    break;
                }
            }
        } while (consume(recordHandler, csvBuffer.buf, csvBuffer.len));

        return fetched;
    }

    @SuppressWarnings("PMD.EmptyIfStmt")
    boolean consume(final RecordHandler rh, final char[] lBuf, final int lLen) {
        int lPos = csvBuffer.pos;
        int lBegin = csvBuffer.begin;
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
                            lStatus |= STATUS_LAST_CHAR_WAS_CR;
                            rh.incLines();
                        } else if (c == LF) {
                            if ((lStatus & STATUS_LAST_CHAR_WAS_CR) == 0) {
                                rh.incLines();
                            } else {
                                lStatus &= ~STATUS_LAST_CHAR_WAS_CR;
                            }
                        } else {
                            // fast-forward
                            while (lPos < lLen) {
                                final char lookAhead = lBuf[lPos++];
                                if (lookAhead == qChar || lookAhead == LF || lookAhead == CR) {
                                    lPos--;
                                    break;
                                }
                            }
                        }
                    }
                } else if ((lStatus & STATUS_COMMENTED_RECORD) != 0) {
                    // commented line
                    while (lPos < lLen) {
                        final char lookAhead = lBuf[lPos++];

                        if (lookAhead == CR) {
                            materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus, qChar);
                            status = STATUS_LAST_CHAR_WAS_CR;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        } else if (lookAhead == LF) {
                            materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus, qChar);
                            status = STATUS_RESET;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        }
                    }
                } else {
                    // we're not in quotes
                    while (lPos < lLen) {
                        final char c = lBuf[lPos++];

                        if (c == fsep) {
                            materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus, qChar);
                            lStatus = STATUS_NEW_FIELD;
                            lBegin = lPos;
                        } else if (c == CR) {
                            materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus, qChar);
                            status = STATUS_LAST_CHAR_WAS_CR;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        } else if (c == LF) {
                            if ((lStatus & STATUS_LAST_CHAR_WAS_CR) == 0) {
                                materialize(lBuf, lBegin, lPos - lBegin - 1, lStatus, qChar);
                                status = STATUS_RESET;
                                lBegin = lPos;
                                moreDataNeeded = false;
                                break OUTER;
                            }

                            lStatus = STATUS_RESET;
                            lBegin = lPos;
                        } else if (cStrat != CommentStrategy.NONE && c == cChar
                            && (lStatus == STATUS_RESET || lStatus == STATUS_LAST_CHAR_WAS_CR)) {
                            lBegin = lPos;
                            lStatus = STATUS_COMMENTED_RECORD;
                            rh.enableCommentMode();
                            continue mode_check;
                        } else if (c == qChar && (lStatus & STATUS_DATA_FIELD) == 0) {
                            // quote and not in data-only mode
                            lStatus = STATUS_QUOTED_FIELD | STATUS_QUOTED_MODE;
                            continue mode_check;
                        } else {
                            if ((lStatus & STATUS_QUOTED_FIELD) == 0) {
                                // normal unquoted data
                                lStatus = STATUS_DATA_FIELD;

                                // fast-forward
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

        csvBuffer.pos = lPos;
        csvBuffer.begin = lBegin;

        return moreDataNeeded;
    }

    private void materialize(final char[] lBuf,
                             final int lBegin, final int lPos, final int lStatus,
                             final char quoteCharacter) {
        if ((lStatus & STATUS_QUOTED_FIELD) == 0) {
            // field without quotes
            recordHandler.add(new String(lBuf, lBegin, lPos), false);
            return;
        }

        // field with quotes
        final int shift = cleanDelimiters(lBuf, lBegin + 1, lBegin + lPos,
            quoteCharacter);
        recordHandler.add(new String(lBuf, lBegin + 1, lPos - 1 - shift), true);
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

    void resetBuffer(final long startingLineNumber) {
        recordHandler.setStartingLineNumber(startingLineNumber);
        csvBuffer.reset();
    }

    @Override
    public void close() throws IOException {
        csvBuffer.close();
    }

    @SuppressWarnings("checkstyle:visibilitymodifier")
    private static class CsvBuffer implements Closeable {
        private static final int READ_SIZE = 8192;
        private static final int BUFFER_SIZE = READ_SIZE;
        private static final int MAX_BUFFER_SIZE = 8 * 1024 * 1024;

        char[] buf;
        int len;
        int begin;
        int pos;

        private final Reader reader;

        CsvBuffer(final Reader reader) {
            this.reader = reader;
            buf = new char[BUFFER_SIZE];
        }

        CsvBuffer(final String data) {
            reader = null;
            buf = data.toCharArray();
            len = data.length();
        }

        /**
         * Reads data from the underlying reader and manages the local buffer.
         *
         * @return {@code true}, if EOD reached.
         * @throws IOException if a read error occurs
         */
        private boolean fetchData() throws IOException {
            if (reader == null) {
                return true;
            }

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

        private static char[] extendAndRelocate(final char[] buf, final int begin)
            throws IOException {

            final int newBufferSize = buf.length * 2;
            if (newBufferSize > MAX_BUFFER_SIZE) {
                throw new IOException("Maximum buffer size " + MAX_BUFFER_SIZE + " is not enough "
                    + "to read data of a single field. Typically, this happens if quotation "
                    + "started but did not end within this buffer's maximum boundary.");
            }
            final char[] newBuf = new char[newBufferSize];
            System.arraycopy(buf, begin, newBuf, 0, buf.length - begin);
            return newBuf;
        }

        private void reset() {
            len = 0;
            begin = 0;
            pos = 0;
        }

        @Override
        public void close() throws IOException {
            if (reader != null) {
                reader.close();
            }
        }

    }

}
