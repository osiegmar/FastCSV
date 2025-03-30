package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

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
    private final boolean acceptCharsAfterQuotes;
    private final CsvCallbackHandler<?> callbackHandler;
    private final CsvBuffer csvBuffer;

    private long startingLineNumber;
    private int lines = 1;

    private int status;
    private boolean finished;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvParser(final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean acceptCharsAfterQuotes,
              final CsvCallbackHandler<?> callbackHandler,
              final int maxBufferSize,
              final Reader reader) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
        this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
        this.callbackHandler = callbackHandler;
        csvBuffer = new CsvBuffer(reader, maxBufferSize);
    }

    CsvParser(final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean acceptCharsAfterQuotes,
              final CsvCallbackHandler<?> callbackHandler,
              final String data) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.fsep = fieldSeparator;
        this.qChar = quoteCharacter;
        this.cStrat = commentStrategy;
        this.cChar = commentCharacter;
        this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
        this.callbackHandler = callbackHandler;
        csvBuffer = new CsvBuffer(data);
    }

    private void assertFields(final char fieldSeparator, final char quoteCharacter, final char commentCharacter) {
        Preconditions.checkArgument(!Util.isNewline(fieldSeparator), "fieldSeparator must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.containsDupe(fieldSeparator, quoteCharacter, commentCharacter),
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
                return processBufferTail();
            }
        } while (consume(csvBuffer.buf, csvBuffer.len));

        // we read data (and passed it to the record handler)
        return true;
    }

    private boolean processBufferTail() {
        if (csvBuffer.begin < csvBuffer.pos) {
            // we have unconsumed data in the buffer
            materialize(csvBuffer.buf, csvBuffer.begin, csvBuffer.pos, status, qChar);
            return true;
        }

        if ((status & STATUS_NEW_FIELD) != 0 || (status & STATUS_COMMENTED_RECORD) != 0) {
            // the last character was a field separator or comment character – add empty field
            materialize(csvBuffer.buf, 0, 0, status, qChar);
            return true;
        }

        // no data left in buffer
        return false;
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
                            for (; lPos < lLen; lPos++) {
                                final char lookAhead = lBuf[lPos];
                                if (lookAhead == qChar || lookAhead == LF || lookAhead == CR) {
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
                            materialize(lBuf, lBegin, lPos - 1, lStatus, qChar);
                            status = STATUS_LAST_CHAR_WAS_CR;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        } else if (lookAhead == LF) {
                            materialize(lBuf, lBegin, lPos - 1, lStatus, qChar);
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
                            materialize(lBuf, lBegin, lPos - 1, lStatus, qChar);
                            lStatus = STATUS_NEW_FIELD;
                            lBegin = lPos;
                        } else if (c == CR) {
                            materialize(lBuf, lBegin, lPos - 1, lStatus, qChar);
                            status = STATUS_LAST_CHAR_WAS_CR;
                            lBegin = lPos;
                            moreDataNeeded = false;
                            break OUTER;
                        } else if (c == LF) {
                            if ((lStatus & STATUS_LAST_CHAR_WAS_CR) == 0) {
                                materialize(lBuf, lBegin, lPos - 1, lStatus, qChar);
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
                                for (; lPos < lLen; lPos++) {
                                    final char lookAhead = lBuf[lPos];
                                    if (lookAhead == fsep || lookAhead == LF || lookAhead == CR) {
                                        break;
                                    }
                                }
                            } else if (!acceptCharsAfterQuotes) {
                                throw new CsvParseException("Unexpected character after closing quote: " + c);
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
            final int beginAfterQuote = lBegin + 1;
            final int endAfterField = lPos - (lBuf[lPos - 1] == quoteCharacter ? 1 : 0);
            callbackHandler.addField(lBuf, beginAfterQuote,
                cleanDelimiters(lBuf, beginAfterQuote, endAfterField, quoteCharacter), true);
            return;
        }

        if ((lStatus & STATUS_COMMENTED_RECORD) != 0) {
            // commented line
            callbackHandler.setComment(lBuf, lBegin, lPos - lBegin);
            return;
        }

        // field without quotes
        callbackHandler.addField(lBuf, lBegin, lPos - lBegin, false);
    }

    /// Remove escapes from the field data.
    ///
    /// The input buffer could look like this: `foo ""is"" bar`
    ///
    /// @param buf            the buffer containing the field data
    /// @param begin          the start position of the field data (after the opening quote)
    /// @param end            the end position of the field data (on the closing quote / end of buffer)
    /// @param quoteCharacter the quote character
    /// @return the length of the field data after removing escapes
    private static int cleanDelimiters(final char[] buf, final int begin, final int end,
                                       final char quoteCharacter) {

        int i = begin;

        // fast-forward to first quote
        while (i < end && buf[i] != quoteCharacter) {
            i++;
        }

        int newPos = i;
        boolean escape = false;
        for (; i < end; i++) {
            final char c = buf[i];
            if (c == quoteCharacter) {
                escape = !escape;
                if (escape) {
                    // skip quote
                    continue;
                }
            }

            // shift character
            buf[newPos++] = c;
        }

        return newPos - begin;
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

    String peekLine() throws IOException {
        final int savedPos = csvBuffer.pos;

        for (; csvBuffer.pos < csvBuffer.len || csvBuffer.fetchData(); csvBuffer.pos++) {
            final char c = csvBuffer.buf[csvBuffer.pos];
            if (c == CR || c == LF) {
                break;
            }
        }

        final String s = new String(csvBuffer.buf, csvBuffer.begin, csvBuffer.pos - csvBuffer.begin);
        csvBuffer.pos = savedPos;
        return s;
    }

    boolean skipLine(final int numCharsToSkip) throws IOException {
        // Skip chars that have been peeked already
        csvBuffer.pos += numCharsToSkip;

        while (csvBuffer.pos < csvBuffer.len || csvBuffer.fetchData()) {
            final char c = csvBuffer.buf[csvBuffer.pos++];
            if (c == CR) {
                if ((csvBuffer.pos < csvBuffer.len || csvBuffer.fetchData())
                    && csvBuffer.buf[csvBuffer.pos] == LF) {
                    // CRLF
                    csvBuffer.pos++;
                }
                break;
            } else if (c == LF) {
                break;
            }
        }

        if (csvBuffer.begin < csvBuffer.pos) {
            csvBuffer.begin = csvBuffer.pos;
            startingLineNumber++;
            return true;
        }

        return false;
    }

    @SuppressWarnings("checkstyle:visibilitymodifier")
    private static class CsvBuffer implements Closeable {

        private static final int DEFAULT_READ_SIZE = 8192;

        char[] buf;
        int len;
        int begin;
        int pos;

        private final Reader reader;
        private final int maxBufferSize;
        private final int readSize;

        CsvBuffer(final Reader reader, final int maxBufferSize) {
            Preconditions.checkArgument(maxBufferSize > 0, "maxBufferSize must be > 0");
            this.reader = reader;
            this.maxBufferSize = maxBufferSize;

            // limit optimal read size to maxBufferSize
            readSize = Math.min(maxBufferSize, DEFAULT_READ_SIZE);

            // Buffer may still contain unprocessed data, so extra space is needed to read readSize chars.
            buf = new char[Math.min(maxBufferSize, readSize * 2)];
        }

        CsvBuffer(final String data) {
            reader = null;
            maxBufferSize = -1;
            buf = data.toCharArray();
            len = data.length();
            readSize = -1;
        }

        /// Reads data from the underlying reader and manages the local buffer.
        ///
        /// @return `true`, if data was fetched, `false` if the end of the stream was reached
        /// @throws IOException if a read error occurs
        private boolean fetchData() throws IOException {
            if (reader == null) {
                // Fixed string data
                return false;
            }

            if (buf.length - len < readSize) {
                // not enough space in the buffer to read readSize chars

                if (begin == len) {
                    // all data was consumed -- nothing to relocate
                    pos = len = 0;
                } else {
                    if (buf.length - len + begin < readSize) {
                        // reclaimable space is insufficient - allocate a larger buffer
                        final char[] newBuf = largerBuffer();
                        System.arraycopy(buf, begin, newBuf, 0, len - begin);
                        buf = newBuf;
                    } else {
                        // it's enough to relocate data and continue with the same buffer
                        System.arraycopy(buf, begin, buf, 0, len - begin);
                    }

                    pos -= begin;
                    len -= begin;
                }

                begin = 0;
            }

            final int cnt = reader.read(buf, len, readSize);
            if (cnt == -1) {
                return false;
            }
            len += cnt;
            return true;
        }

        private char[] largerBuffer() {
            if (maxBufferSize == buf.length) {
                throw new CsvParseException(String.format("The maximum buffer size of %d is "
                        + "insufficient to read the data of a single field. "
                        + "This issue typically arises when a quotation begins but does not conclude within the "
                        + "confines of this buffer's maximum limit.",
                    maxBufferSize));
            }
            return new char[Math.min(maxBufferSize, buf.length * 2)];
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
