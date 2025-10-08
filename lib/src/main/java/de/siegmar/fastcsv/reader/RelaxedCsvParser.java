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

/// Less strict but also less performant CSV parser.
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
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        this.reader = new LookaheadReader(reader, DEFAULT_BUFFER_SIZE);
        currentField = new char[Math.min(maxBufferSize, DEFAULT_BUFFER_SIZE)];
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    RelaxedCsvParser(final String fsep, final char qChar,
                     final CommentStrategy cStrat, final char cChar,
                     final boolean trimWhitespacesAroundQuotes,
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
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        reader = new LookaheadReader(new StringReader(data), Math.max(data.length(), 1));
        currentField = new char[Math.min(maxBufferSize, data.length())];
    }

    private static void assertFields(final String fieldSeparator, final char quoteCharacter,
                              final char commentCharacter, final CommentStrategy commentStrategy) {
        Preconditions.checkArgument(!Util.containsNewline(fieldSeparator),
            "fieldSeparator must not contain newline chars");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");

        if (commentStrategy == CommentStrategy.NONE) {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator.charAt(0), quoteCharacter),
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s)".formatted(
                    fieldSeparator.charAt(0), quoteCharacter));
        } else {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator.charAt(0), quoteCharacter, commentCharacter),
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)".formatted(
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

    @SuppressWarnings({"checkstyle:ReturnCount", "checkstyle:NPathComplexity"})
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
        "checkstyle:BooleanExpressionComplexity",
        "PMD.AvoidReassigningParameters"
    })
    private boolean parseUnquoted(int ch) throws IOException {
        boolean endOfRecord = true;

        do {
            // fast-forward
            while (currentFieldIndex < currentField.length && reader.len > reader.start
                && ch != CR && ch != LF && ch != fsep && ch != qChar) {
                currentField[currentFieldIndex++] = (char) ch;
                ch = reader.buffer[reader.start++];
            }

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
            if (ch == qChar && trimWhitespacesAroundQuotes && currentFieldHasOnlyWhitespace()) {
                currentFieldIndex = 0;
                return parseQuoted();
            }

            appendChar(ch);
        } while ((ch = reader.read()) != EOF);

        callbackHandler.addField(currentField, 0, currentFieldIndex, false);
        currentFieldIndex = 0;
        return endOfRecord;
    }

    private boolean currentFieldHasOnlyWhitespace() {
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
        OUTER: while ((ch = reader.read()) != EOF) {
            // fast-forward
            while (currentFieldIndex < currentField.length && reader.len > reader.start
                && ch != CR && ch != LF && ch != qChar) {
                currentField[currentFieldIndex++] = (char) ch;
                ch = reader.buffer[reader.start++];
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
                        throw new CsvParseException("Unexpected character after closing quote: '%c' (0x%x)"
                            .formatted(ch, ch));
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
        }

        callbackHandler.setComment(currentField, 0, currentFieldIndex);
        currentFieldIndex = 0;
    }

    private void appendChar(final int ch) {
        if (currentField.length == currentFieldIndex) {
            if (currentField.length == maxBufferSize) {
                throw new CsvParseException("""
                    The maximum buffer size of %d is \
                    insufficient to read the data of a single field. \
                    This issue typically arises when a quotation begins but does not conclude within the \
                    confines of this buffer's maximum limit. \
                    """.formatted(maxBufferSize));
            }
            final char[] newField = new char[Math.min(maxBufferSize, currentField.length * 2)];
            System.arraycopy(currentField, 0, newField, 0, currentField.length);
            currentField = newField;
        }

        currentField[currentFieldIndex++] = (char) ch;
    }

    @Override
    public String peekLine() throws IOException {
        return reader.peekLine();
    }

    @SuppressWarnings("checkstyle:MultipleVariableDeclarations")
    @Override
    public void skipLine(final int numCharsToSkip) throws IOException {
        reader.skip(numCharsToSkip);

        int c = reader.read();
        if (c == EOF) {
            if (numCharsToSkip == 0) {
                throw new EOFException();
            }
            return;
        }

        do {
            if (c == CR) {
                reader.consumeLF();
                startingLineNumber++;
                break;
            }
            if (c == LF) {
                startingLineNumber++;
                break;
            }
        } while ((c = reader.read()) != EOF);
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

    private static final class LookaheadReader implements Closeable {

        private final Reader reader;
        private final char[] buffer;
        private int start;
        private int len;

        LookaheadReader(final Reader reader, final int bufferSize) {
            this.reader = reader;
            buffer = new char[bufferSize];
        }

        int read() throws IOException {
            ensureBuffered(1);
            return start >= len ? -1 : buffer[start++];
        }

        boolean consumeLF() throws IOException {
            ensureBuffered(1);
            if (start >= len || buffer[start] != LF) {
                return false;
            }
            start++;
            return true;
        }

        @SuppressWarnings("PMD.UseVarargs")
        boolean consumeIf(final char[] chars) throws IOException {
            ensureBuffered(chars.length);
            if (len - start < chars.length) {
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
            ensureBuffered(buffer.length);
            if (start >= len) {
                throw new EOFException();
            }
            int endIndex = start;
            while (endIndex < len && buffer[endIndex] != CR && buffer[endIndex] != LF) {
                endIndex++;
            }
            return new String(buffer, start, endIndex - start);
        }

        private void ensureBuffered(final int required) throws IOException {
            final int available = len - start;
            if (len == -1 || required <= available) {
                return;
            }

            // relocate the buffer if necessary
            if (start > 0 && required > buffer.length - start) {
                final int remaining = len - start;
                System.arraycopy(buffer, start, buffer, 0, remaining);
                start = 0;
                len = remaining;
            }

            // fetch more data
            while (len - start < required) {
                final int count = reader.read(buffer, len, buffer.length - len);
                if (count == -1) {
                    len = (start >= len) ? -1 : len;
                    break;
                }
                len += count;
            }
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
