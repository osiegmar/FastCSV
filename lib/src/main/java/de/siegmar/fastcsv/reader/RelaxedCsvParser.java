package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/// Less strict but also less performant CSV parser.
@SuppressWarnings({
    "checkstyle:CyclomaticComplexity",
    "checkstyle:ExecutableStatementCount",
    "checkstyle:InnerAssignment",
    "checkstyle:JavaNCSS",
    "checkstyle:NestedIfDepth",
    "PMD.UnusedAssignment"
})
final class RelaxedCsvParser implements CsvParser {

    private static final char SPACE = ' ';
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final char[] fsep;
    private final char qChar;
    private final CommentStrategy cStrat;
    private final char cChar;
    private final boolean trimWhitespacesAroundQuotes;
    private final CsvCallbackHandler<?> callbackHandler;
    private final int maxBufferSize;
    private final Reader reader;
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
        assertFields(fsep, qChar, cChar);

        this.fsep = fsep.toCharArray();
        this.qChar = qChar;
        this.cStrat = cStrat;
        this.cChar = cChar;
        this.trimWhitespacesAroundQuotes = trimWhitespacesAroundQuotes;
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        this.reader = new BufferedReader(reader);
        currentField = new char[Math.min(maxBufferSize, DEFAULT_BUFFER_SIZE)];
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    RelaxedCsvParser(final String fsep, final char qChar,
                     final CommentStrategy cStrat, final char cChar,
                     final boolean trimWhitespacesAroundQuotes,
                     final CsvCallbackHandler<?> callbackHandler,
                     final int maxBufferSize,
                     final String data) {
        assertFields(fsep, qChar, cChar);

        this.fsep = fsep.toCharArray();
        this.qChar = qChar;
        this.cStrat = cStrat;
        this.cChar = cChar;
        this.trimWhitespacesAroundQuotes = trimWhitespacesAroundQuotes;
        this.callbackHandler = callbackHandler;
        this.maxBufferSize = maxBufferSize;
        reader = new StringReader(data);
        currentField = new char[Math.min(maxBufferSize, data.length())];
    }

    private void assertFields(final String fieldSeparator, final char quoteCharacter, final char commentCharacter) {
        for (final char fs : fieldSeparator.toCharArray()) {
            Preconditions.checkArgument(!Util.isNewline(fs), "fieldSeparator must not be a newline char");
        }
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.containsDupe(fieldSeparator.charAt(0), quoteCharacter, commentCharacter),
            "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
            fieldSeparator.charAt(0), quoteCharacter, commentCharacter);
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    @Override
    public boolean parse() throws IOException {
        startingLineNumber += lines;
        lines = 1;
        callbackHandler.beginRecord(startingLineNumber);

        int ch = reader.read();

        if (ch == EOF) {
            return currentFieldIndex > 0 && materializeField(true, false);
        }

        if (ch == CR) {
            reader.mark(1);
            if (reader.read() != LF) {
                reader.reset();
            }
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

        return materializeField(true, false);
    }

    @SuppressWarnings({
        "checkstyle:ReturnCount",
        "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment",
        "PMD.AvoidReassigningParameters"
    })
    private boolean parseUnquoted(int ch) throws IOException {
        do {
            if (ch == fsep[0] && (fsep.length == 1 || fieldSeparatorMatch())) {
                return materializeField(false, false);
            }
            if (ch == CR) {
                reader.mark(1);
                if (reader.read() != LF) {
                    reader.reset();
                }
                return materializeField(true, false);
            }
            if (ch == LF) {
                return materializeField(true, false);
            }
            if (ch == qChar && trimWhitespacesAroundQuotes && currentFieldHasOnlyWhitespace()) {
                currentFieldIndex = 0;
                return parseQuoted();
            }

            appendChar(ch);
        } while ((ch = reader.read()) != EOF);

        return materializeField(true, false);
    }

    private boolean fieldSeparatorMatch() throws IOException {
        reader.mark(fsep.length - 1);
        for (int i = 1; i < fsep.length; i++) {
            if (fsep[i] != reader.read()) {
                reader.reset();
                return false;
            }
        }

        return true;
    }

    private boolean currentFieldHasOnlyWhitespace() {
        for (int i = 0; i < currentFieldIndex; i++) {
            if (currentField[i] > SPACE) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:ReturnCount", "PMD.AssignmentInOperand"})
    private boolean parseQuoted() throws IOException {
        int ch;
        while ((ch = reader.read()) != EOF) {
            if (ch == CR) {
                appendChar(ch);
                reader.mark(1);
                if (reader.read() == LF) {
                    appendChar(LF);
                } else {
                    reader.reset();
                }
                lines++;
            } else if (ch == LF) {
                appendChar(ch);
                lines++;
            } else if (ch == qChar) {
                reader.mark(1);
                int lookAhead = reader.read();
                if (lookAhead != qChar) {
                    // closing quote
                    for (; lookAhead != EOF; lookAhead = reader.read()) {
                        if (lookAhead == CR) {
                            // CR right after closing quote
                            reader.mark(1);
                            if (reader.read() != LF) {
                                reader.reset();
                            }
                            return materializeField(true, true);
                        }
                        if (lookAhead == LF) {
                            // LF right after closing quote
                            return materializeField(true, true);
                        }
                        if (lookAhead == fsep[0] && (fsep.length == 1 || fieldSeparatorMatch())) {
                            // field separator after closing quote
                            return materializeField(false, true);
                        }
                        if (!trimWhitespacesAroundQuotes || lookAhead > SPACE) {
                            throw new CsvParseException("Unexpected character after closing quote: '%c' (0x%x)"
                                .formatted(lookAhead, lookAhead));
                        }
                    }

                    return materializeField(true, true);
                } else {
                    appendChar(ch);
                }
            } else {
                appendChar(ch);
            }
        }

        // EOF before closing quote
        return materializeField(true, true);
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private void parseComment() throws IOException {
        int ch;
        while ((ch = reader.read()) != EOF && ch != CR && ch != LF) {
            appendChar(ch);
        }
        if (ch == CR) {
            reader.mark(1);
            if (reader.read() != LF) {
                reader.reset();
            }
        }

        callbackHandler.setComment(currentField, 0, currentFieldIndex);
        currentFieldIndex = 0;
    }

    private void appendChar(final int ch) {
        if (currentField.length == currentFieldIndex + 1) {
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

    private boolean materializeField(final boolean endOfRecord, final boolean quoted) {
        callbackHandler.addField(currentField, 0, currentFieldIndex, quoted);
        currentFieldIndex = 0;
        return endOfRecord;
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    @Override
    public String peekLine() throws IOException {
        reader.mark(DEFAULT_BUFFER_SIZE);

        int c;
        while ((c = reader.read()) != EOF && c != CR && c != LF) {
            appendChar(c);
        }
        reader.reset();

        final String s = new String(currentField, 0, currentFieldIndex);
        currentFieldIndex = 0;
        return s;
    }

    @SuppressWarnings({
        "checkstyle:MultipleVariableDeclarations",
        "PMD.OneDeclarationPerLine",
        "PMD.AssignmentInOperand"
    })
    @Override
    public boolean skipLine(final int numCharsToSkip) throws IOException {
        if (reader.skip(numCharsToSkip) < numCharsToSkip) {
            throw new IllegalStateException("Could not skip " + numCharsToSkip + " characters");
        }

        int i, c;
        for (i = 0; (c = reader.read()) != EOF; i++) {
            if (c == CR) {
                reader.mark(1);
                if (reader.read() != LF) {
                    reader.reset();
                }
                startingLineNumber++;
                return true;
            }
            if (c == LF) {
                startingLineNumber++;
                return true;
            }
        }

        return numCharsToSkip + i > 0;
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

}
