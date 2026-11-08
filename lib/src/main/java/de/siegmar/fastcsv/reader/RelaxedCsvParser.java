package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import de.siegmar.fastcsv.util.Nullable;
import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/// CSV parser supporting multi-character field separators and tolerances such as whitespace around quote
/// characters; less performant than [StrictCsvParser].
@SuppressWarnings({
    "checkstyle:CyclomaticComplexity",
    "checkstyle:ExecutableStatementCount",
    "checkstyle:InnerAssignment",
    "checkstyle:JavaNCSS",
    "checkstyle:NestedIfDepth"
})
final class RelaxedCsvParser implements CsvParser {

    private static final char SPACE = ' ';
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final char fsep;

    @Nullable
    private final char[] fsepRemainder;

    private final char qChar;
    private final CommentStrategy cStrat;
    private final char cChar;
    private final boolean trimWhitespacesAroundQuotes;
    private final boolean allowUnclosedQuote;
    private final CsvCallbackHandler<?> callbackHandler;
    private final int maxBufferSize;
    private final LookaheadReader reader;
    private long startingLineNumber;
    private char[] currentField;
    private int currentFieldIndex;
    private int lines = 1;

    @SuppressWarnings("checkstyle:ParameterNumber")
    RelaxedCsvParser(final String fsep, final char qChar,
                     final CommentStrategy cStrat, final char cChar,
                     final boolean trimWhitespacesAroundQuotes,
                     final boolean allowUnclosedQuote,
                     final CsvCallbackHandler<?> callbackHandler,
                     final int maxBufferSize,
                     final Reader reader) {
        assertFields(fsep, qChar, cChar, cStrat);

        this.fsep = fsep.charAt(0);
        fsepRemainder = extractFsepRemainder(fsep);
        this.qChar = qChar;
        this.cStrat = cStrat;
        this.cChar = cChar;
        this.trimWhitespacesAroundQuotes = trimWhitespacesAroundQuotes;
        this.allowUnclosedQuote = allowUnclosedQuote;
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        final int initialBufferSize = Math.min(maxBufferSize, DEFAULT_BUFFER_SIZE);
        this.reader = new LookaheadReader(reader, initialBufferSize, maxBufferSize);
        currentField = new char[initialBufferSize];
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    RelaxedCsvParser(final String fsep, final char qChar,
                     final CommentStrategy cStrat, final char cChar,
                     final boolean trimWhitespacesAroundQuotes,
                     final boolean allowUnclosedQuote,
                     final CsvCallbackHandler<?> callbackHandler,
                     final int maxBufferSize,
                     final String data) {
        assertFields(fsep, qChar, cChar, cStrat);

        this.fsep = fsep.charAt(0);
        fsepRemainder = extractFsepRemainder(fsep);
        this.qChar = qChar;
        this.cStrat = cStrat;
        this.cChar = cChar;
        this.trimWhitespacesAroundQuotes = trimWhitespacesAroundQuotes;
        this.allowUnclosedQuote = allowUnclosedQuote;
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        final int dataSize = Math.max(data.length(), 1);
        reader = new LookaheadReader(new StringReader(data), dataSize, dataSize);
        currentField = new char[Math.min(maxBufferSize, data.length())];
    }

    private static void assertFields(final String fieldSeparator, final char quoteCharacter,
                              final char commentCharacter, final CommentStrategy commentStrategy) {
        Preconditions.checkArgument(!Util.containsLineBreak(fieldSeparator),
            "fieldSeparator must not contain newline chars");
        Preconditions.checkArgument(!Util.isLineBreak(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isLineBreak(commentCharacter), "commentCharacter must not be a newline char");

        if (commentStrategy == CommentStrategy.NONE) {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator.charAt(0), quoteCharacter), () ->
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s)".formatted(
                    fieldSeparator.charAt(0), quoteCharacter));
        } else {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator.charAt(0), quoteCharacter, commentCharacter),
                () -> "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)"
                    .formatted(
                    fieldSeparator.charAt(0), quoteCharacter, commentCharacter));
        }
    }

    @Nullable
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.ReturnEmptyCollectionRatherThanNull"})
    private static char[] extractFsepRemainder(final String fsep) {
        if (fsep.length() <= 1) {
            return null;
        }
        final char[] fsepRemainder = new char[fsep.length() - 1];
        fsep.getChars(1, fsep.length(), fsepRemainder, 0);
        return fsepRemainder;
    }

    @SuppressWarnings({"checkstyle:ReturnCount", "checkstyle:NPathComplexity", "PMD.AssignmentInOperand"})
    @Override
    public boolean parse() throws IOException {
        startingLineNumber += lines;
        lines = 1;
        callbackHandler.beginRecord(startingLineNumber);

        int ch = reader.read();

        if (ch == EOF) {
            return false;
        }
        if (ch == CR) {
            reader.consumeLF();
            callbackHandler.setEmpty();
            return true;
        }
        if (ch == LF) {
            callbackHandler.setEmpty();
            return true;
        }
        if (ch == cChar && cStrat != CommentStrategy.NONE) {
            parseComment();
            return true;
        }

        do {
            if (ch == qChar ? parseQuoted() : parseUnquoted(ch)) {
                return true;
            }
        } while ((ch = reader.read()) != EOF);

        callbackHandler.addField(currentField, 0, currentFieldIndex, false);
        currentFieldIndex = 0;
        return true;
    }

    @SuppressWarnings({
        "checkstyle:ReturnCount",
        "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment",
        "checkstyle:NPathComplexity",
        "PMD.AvoidReassigningParameters",
        "PMD.AssignmentInOperand"
    })
    private boolean parseUnquoted(int ch) throws IOException {
        boolean endOfRecord = true;

        do {
            if (ch == fsep && (fsepRemainder == null || reader.consumeIf(fsepRemainder))) {
                endOfRecord = false;
                break;
            }
            if (ch == CR) {
                reader.consumeLF();
                break;
            }
            if (ch == LF) {
                break;
            }
            if (ch == qChar && trimWhitespacesAroundQuotes && fieldContainsOnlyWhitespace()) {
                currentFieldIndex = 0;
                return parseQuoted();
            }

            appendChar(ch);
            appendRun(findRunEnd(true));
        } while ((ch = reader.read()) != EOF);

        callbackHandler.addField(currentField, 0, currentFieldIndex, false);
        currentFieldIndex = 0;
        return endOfRecord;
    }

    /// Finds the end of the longest run of buffered characters that are plain data,
    /// starting at `reader.start`. CR/LF and the quote character always end the run;
    /// in unquoted fields, the field separator does as well.
    ///
    /// Characters `<= CR` other than CR/LF also end the run although they are plain data --
    /// the caller simply appends them one by one and scans again.
    ///
    /// @return the buffer position after the last plain character
    private int findRunEnd(final boolean unquoted) {
        final char[] buf = reader.buffer;
        final int bufLen = reader.len;
        int i;
        for (i = reader.start; i < bufLen; i++) {
            final char c = buf[i];
            if (c <= CR || c == qChar || (unquoted && c == fsep)) {
                break;
            }
        }
        return i;
    }

    /// Appends the buffered characters between `reader.start` and `end` (exclusive)
    /// to the current field, consuming them from the reader.
    private void appendRun(final int end) {
        final int from = reader.start;
        final int runLen = end - from;
        if (runLen > 0) {
            if (runLen > currentField.length - currentFieldIndex) {
                growField((long) currentFieldIndex + runLen);
            }
            System.arraycopy(reader.buffer, from, currentField, currentFieldIndex, runLen);
            currentFieldIndex += runLen;
            reader.start = end;
        }
    }

    private boolean fieldContainsOnlyWhitespace() {
        for (int i = 0; i < currentFieldIndex; i++) {
            if (currentField[i] > SPACE) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({
        "checkstyle:NPathComplexity",
        "checkstyle:ReturnCount",
        "checkstyle:BooleanExpressionComplexity",
        "PMD.AssignmentInOperand"
    })
    private boolean parseQuoted() throws IOException {
        boolean endOfRecord = true;

        int ch;
        OUTER: while (true) {
            ch = reader.read();
            if (ch == EOF) {
                if (!allowUnclosedQuote) {
                    throw new CsvParseException(
                        "Unclosed quoted field at end of input (record starting at line %d)"
                            .formatted(startingLineNumber));
                }
                break;
            }

            if (ch == qChar && (ch = reader.read()) != qChar) {
                // closing quote
                for (; ch != EOF; ch = reader.read()) {
                    if (ch == CR) {
                        // CR right after closing quote
                        reader.consumeLF();
                        break OUTER;
                    }
                    if (ch == LF) {
                        // LF right after closing quote
                        break OUTER;
                    }
                    if (ch == fsep && (fsepRemainder == null || reader.consumeIf(fsepRemainder))) {
                        // field separator after closing quote
                        endOfRecord = false;
                        break OUTER;
                    }
                    if (!trimWhitespacesAroundQuotes || ch > SPACE) {
                        throw new CsvParseException(Util.escapeControlCharacters(
                            "Unexpected character after closing quote: '%c' (0x%x) (record starting at line %d)"
                                .formatted(ch, ch, startingLineNumber)));
                    }
                }

                break;
            }

            appendChar(ch);
            if (ch == CR) {
                if (reader.consumeLF()) {
                    appendChar(LF);
                }
                lines++;
            } else if (ch == LF) {
                lines++;
            } else {
                appendRun(findRunEnd(false));
            }
        }

        callbackHandler.addField(currentField, 0, currentFieldIndex, true);
        currentFieldIndex = 0;
        return endOfRecord;
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private void parseComment() throws IOException {
        int ch;
        while ((ch = reader.read()) != EOF && ch != LF) {
            if (ch == CR) {
                reader.consumeLF();
                break;
            }
            appendChar(ch);
            appendRun(findRunEnd(false));
        }

        callbackHandler.setComment(currentField, 0, currentFieldIndex);
        currentFieldIndex = 0;
    }

    private void appendChar(final int ch) {
        if (currentField.length == currentFieldIndex) {
            growField(currentFieldIndex + 1L);
        }

        currentField[currentFieldIndex++] = (char) ch;
    }

    private void growField(final long requiredSize) {
        if (requiredSize > maxBufferSize) {
            throw new CsvParseException("""
                The maximum buffer size of %d is \
                insufficient to read the data of a single field. \
                This issue typically arises when a quotation begins but does not conclude within the \
                confines of this buffer's maximum limit. \
                (record starting at line %d)""".formatted(maxBufferSize, startingLineNumber));
        }
        final long newSize = Math.min(maxBufferSize, Math.max(currentField.length * 2L, requiredSize));
        final char[] newField = new char[(int) newSize];
        System.arraycopy(currentField, 0, newField, 0, currentFieldIndex);
        currentField = newField;
    }

    @Override
    public String peekLine() throws IOException {
        return reader.peekLine();
    }

    @SuppressWarnings({"checkstyle:MultipleVariableDeclarations", "PMD.AssignmentInOperand"})
    @Override
    public void skipLine(final int numCharsToSkip) throws IOException {
        reader.skip(numCharsToSkip);

        int c = reader.read();
        if (c == EOF) {
            if (numCharsToSkip == 0) {
                throw new EOFException();
            }
            // An unterminated last line was skipped: count it like any other skipped line.
            startingLineNumber++;
            return;
        }

        do {
            if (c == CR) {
                reader.consumeLF();
                break;
            }
            if (c == LF) {
                break;
            }
        } while ((c = reader.read()) != EOF);

        // Count the skipped line, whether it ended with a line break or with the end of data.
        startingLineNumber++;
    }

    @Override
    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void reset(final long startingLineNumber) {
        // The IndexedCsvReader currently does not support relaxed parsing.
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private final class LookaheadReader implements Closeable {

        private final Reader reader;
        private final int maxBufferSize;
        private char[] buffer;
        private int start;
        private int len;

        LookaheadReader(final Reader reader, final int bufferSize, final int maxBufferSize) {
            this.reader = reader;
            this.maxBufferSize = maxBufferSize;
            buffer = new char[bufferSize];
        }

        int read() throws IOException {
            return ensureBuffered(1) ? buffer[start++] : -1;
        }

        boolean consumeLF() throws IOException {
            if (!ensureBuffered(1) || buffer[start] != LF) {
                return false;
            }
            start++;
            return true;
        }

        @SuppressWarnings("PMD.UseVarargs")
        boolean consumeIf(final char[] chars) throws IOException {
            if (!ensureBuffered(chars.length)) {
                return false;
            }
            for (int i = 0; i < chars.length; i++) {
                if (buffer[start + i] != chars[i]) {
                    return false;
                }
            }
            start += chars.length;
            return true;
        }

        String peekLine() throws IOException {
            if (!ensureBuffered(1)) {
                throw new EOFException();
            }
            int scan = 0;
            do {
                while (start + scan < len
                    && buffer[start + scan] != CR && buffer[start + scan] != LF) {
                    scan++;
                }
            } while (start + scan >= len && ensureBuffered(scan + 1));
            return new String(buffer, start, scan);
        }

        /// Ensures `required` characters are available in the buffer.
        /// Returns `true` if so, `false` if EOF was reached before that count was met.
        private boolean ensureBuffered(final int required) throws IOException {
            if (len == -1) {
                return false;
            }
            final int available = len - start;
            if (required <= available) {
                return true;
            }

            if (required > buffer.length) {
                // grow (relocates `start` to 0 along the way)
                if (required > maxBufferSize) {
                    throw new CsvParseException(
                        "The maximum buffer size of %d is insufficient to read a single line (line %d)."
                            .formatted(maxBufferSize, startingLineNumber + 1));
                }
                final char[] newBuf = new char[Math.min(maxBufferSize, Math.max(buffer.length * 2, required))];
                System.arraycopy(buffer, start, newBuf, 0, available);
                buffer = newBuf;
                start = 0;
                len = available;
            } else if (required > buffer.length - start) {
                // compact
                System.arraycopy(buffer, start, buffer, 0, available);
                start = 0;
                len = available;
            }

            // fetch more data
            while (len - start < required) {
                final int count = reader.read(buffer, len, buffer.length - len);
                if (count == -1) {
                    if (start >= len) {
                        len = -1;
                    }
                    break;
                }
                len += count;
            }
            return len - start >= required;
        }

        void skip(final int numCharsToSkip) {
            start += numCharsToSkip;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

}
