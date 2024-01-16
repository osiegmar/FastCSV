package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;
import static de.siegmar.fastcsv.util.Util.containsDupe;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import de.siegmar.fastcsv.util.Limits;
import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

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
final class CsvParser implements Closeable {

    private static final int STATUS_LAST_CHAR_WAS_CR = 32;
    private static final int STATUS_COMMENTED_RECORD = 16;
    private static final int STATUS_NEW_FIELD = 8;
    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_FIELD = 2;
    private static final int STATUS_DATA_FIELD = 1;
    private static final int STATUS_RESET = 0;

    private final char fsep;
    private final char qChar;
    private final CommentStrategy cStrat;
    private final char cChar;
    private final CsvCallbackHandler<?> callbackHandler;
    private final CsvBuffer csvBuffer;

    private long startingLineNumber;
    private int lines = 1;

    private int status;
    private boolean finished;

    CsvParser(final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final CsvCallbackHandler<?> callbackHandler, final Reader reader) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
        this.callbackHandler = callbackHandler;
        csvBuffer = new CsvBuffer(reader);
    }

    CsvParser(final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final CsvCallbackHandler<?> callbackHandler, final String data) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
        this.callbackHandler = callbackHandler;
        csvBuffer = new CsvBuffer(data);
    }

    private void assertFields(final char fieldSeparator, final char quoteCharacter, final char commentCharacter) {
        Preconditions.checkArgument(!Util.isNewline(fieldSeparator), "fieldSeparator must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!containsDupe(fieldSeparator, quoteCharacter, commentCharacter),
            "Control characters must differ"
                + " (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
            fieldSeparator, quoteCharacter, commentCharacter);
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    boolean parse() throws IOException {
        if (finished) {
            // no more data available
            return false;
        }

        startingLineNumber += lines;
        lines = 1;
        callbackHandler.beginRecord(startingLineNumber);

        do {
            if (csvBuffer.len == csvBuffer.pos && !csvBuffer.fetchData()) {
                // buffer is processed and no more data available
                finished = true;

                if (csvBuffer.begin < csvBuffer.pos) {
                    // we have unconsumed data in the buffer
                    materialize(csvBuffer.buf, csvBuffer.begin, csvBuffer.pos - csvBuffer.begin, status, qChar);
                    return true;
                }

                if ((status & STATUS_NEW_FIELD) != 0 || (status & STATUS_COMMENTED_RECORD) != 0) {
                    // last character was a field separator or comment character – add empty field
                    materialize(csvBuffer.buf, 0, 0, status, qChar);
                    return true;
                }

                // no data left in buffer
                return false;
            }
        } while (consume(csvBuffer.buf, csvBuffer.len));

        // we read data (and passed it to the record handler)
        return true;
    }

    @SuppressWarnings("PMD.EmptyIfStmt")
    boolean consume(final char[] lBuf, final int lLen) {
        int lPos = csvBuffer.pos;
        int lBegin = csvBuffer.begin;
        int lStatus = status;
        boolean moreDataNeeded = true;

        OUTER:
        {
            mode_check:
            do {
                if ((lStatus & STATUS_QUOTED_MODE) != 0) {
                    // we're in quotes
                    while (lPos < lLen) {
                        final char c = lBuf[lPos++];

                        if (c == qChar) {
                            lStatus &= ~STATUS_QUOTED_MODE;
                            continue mode_check;
                        } else if (c == CR) {
                            lStatus |= STATUS_LAST_CHAR_WAS_CR;
                            lines++;
                        } else if (c == LF) {
                            if ((lStatus & STATUS_LAST_CHAR_WAS_CR) == 0) {
                                lines++;
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

        if ((lStatus & STATUS_QUOTED_FIELD) != 0) {
            // field with quotes
            final int shift = cleanDelimiters(lBuf, lBegin + 1, lBegin + lPos, quoteCharacter);
            callbackHandler.addField(lBuf, lBegin + 1, lPos - 1 - shift, true);
            return;
        }

        if ((lStatus & STATUS_COMMENTED_RECORD) != 0) {
            // commented line
            callbackHandler.setComment(lBuf, lBegin, lPos);
            return;
        }

        // field without quotes
        callbackHandler.addField(lBuf, lBegin, lPos, false);
    }

    private static int cleanDelimiters(final char[] buf, final int begin, final int pos,
                                       final char quoteCharacter) {

        int i = begin;

        // fast-forward to first quote
        while (i < pos && buf[i] != quoteCharacter) {
            i++;
        }

        int shift = 0;
        boolean escape = false;
        for (; i < pos; i++) {
            final char c = buf[i];

            if (c == quoteCharacter) {
                if (!escape) {
                    shift++;
                    escape = true;
                    continue;
                }
                escape = false;
            }

            if (shift > 0) {
                buf[i - shift] = c;
            }
        }

        return shift;
    }

    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    @SuppressWarnings("checkstyle:HiddenField")
    void reset(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
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
         * @return {@code true}, if data was fetched, {@code false} if the end of the stream was reached
         * @throws IOException if a read error occurs
         */
        private boolean fetchData() throws IOException {
            if (reader == null) {
                return false;
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
                return false;
            }
            len = pos + cnt;
            return true;
        }

        private static char[] extendAndRelocate(final char[] buf, final int begin) {
            final int newBufferSize = buf.length * 2;
            if (newBufferSize > Limits.MAX_FIELD_SIZE) {
                throw new CsvParseException(String.format("The maximum buffer size of %d is "
                        + "insufficient to read the data of a single field. "
                        + "This issue typically arises when a quotation begins but does not conclude within the "
                        + "confines of this buffer's maximum limit.",
                    Limits.MAX_FIELD_SIZE));
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
