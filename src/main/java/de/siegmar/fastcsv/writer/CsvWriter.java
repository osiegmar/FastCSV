package de.siegmar.fastcsv.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * This is the main class for writing CSV data.
 *
 * Example use:
 * <pre>{@code
 * try (CsvWriter csv = CsvWriter.builder().build(file, StandardCharsets.UTF_8)) {
 *     csv.writeLine("Hello", "world");
 * }
 * }</pre>
 */
public class CsvWriter implements Closeable {

    private static final char CR = '\r';
    private static final char LF = '\n';

    private final CachingWriter writer;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final QuoteStrategy quoteStrategy;
    private final String lineDelimiter;
    private final boolean earlyFlush;

    private boolean isNewline = true;

    CsvWriter(final Writer writer, final char fieldSeparator, final char quoteCharacter,
              final QuoteStrategy quoteStrategy, final LineDelimiter lineDelimiter,
              final boolean earlyFlush) {
        this.writer = new CachingWriter(writer);
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.quoteStrategy = Objects.requireNonNull(quoteStrategy);
        this.lineDelimiter = Objects.requireNonNull(lineDelimiter).toString();
        this.earlyFlush = earlyFlush;
    }

    /**
     * Creates a {@link CsvWriterBuilder} instance used to configure and create instances of
     * this class.
     * @return CsvWriterBuilder instance with default settings.
     */
    public static CsvWriterBuilder builder() {
        return new CsvWriterBuilder();
    }

    /**
     * Appends a field to the current row. Automatically adds field separator and quotes as
     * required.
     *
     * @param value the field to append (can be {@code null})
     * @return This CsvWriter.
     * @throws IOException if a write error occurs
     */
    public CsvWriter writeField(final String value) throws IOException {
        writeInternal(value);
        if (earlyFlush) {
            writer.flushBuffer();
        }
        return this;
    }

    private void writeInternal(final String value) throws IOException {
        if (!isNewline) {
            writer.write(fieldSeparator);
        } else {
            isNewline = false;
        }

        if (value == null) {
            if (quoteStrategy == QuoteStrategy.ALWAYS) {
                writer.write(quoteCharacter);
                writer.write(quoteCharacter);
            }
            return;
        }

        if (value.isEmpty()) {
            if (quoteStrategy == QuoteStrategy.ALWAYS
                || quoteStrategy == QuoteStrategy.EMPTY) {
                writer.write(quoteCharacter);
                writer.write(quoteCharacter);
            }
            return;
        }

        final int length = value.length();
        boolean needsQuotes = quoteStrategy == QuoteStrategy.ALWAYS;
        int nextDelimPos = -1;

        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c == quoteCharacter) {
                needsQuotes = true;
                nextDelimPos = i;
                break;
            }
            if (!needsQuotes && (c == fieldSeparator || c == LF || c == CR)) {
                needsQuotes = true;
            }
        }

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }

        if (nextDelimPos > -1) {
            writeEscaped(value, length, nextDelimPos);
        } else {
            writer.write(value, 0, length);
        }

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    private void writeEscaped(final String value, final int length, int nextDelimPos)
        throws IOException {

        int startPos = 0;
        do {
            final int len = nextDelimPos - startPos + 1;
            writer.write(value, startPos, len);
            writer.write(quoteCharacter);
            startPos += len;

            nextDelimPos = -1;
            for (int i = startPos; i < length; i++) {
                if (value.charAt(i) == quoteCharacter) {
                    nextDelimPos = i;
                    break;
                }
            }
        } while (nextDelimPos > -1);

        if (length > startPos) {
            writer.write(value, startPos, length - startPos);
        }
    }

    /**
     * Appends a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to append ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY}))
     * @return This CsvWriter.
     * @throws IOException if a write error occurs
     */
    public CsvWriter writeLine(final Iterable<String> values) throws IOException {
        for (final String value : values) {
            writeInternal(value);
        }
        endLine();
        return this;
    }

    /**
     * Appends a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to append ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY}))
     * @return This CsvWriter.
     * @throws IOException if a write error occurs
     */
    public CsvWriter writeLine(final String... values) throws IOException {
        for (final String value : values) {
            writeInternal(value);
        }
        endLine();
        return this;
    }

    /**
     * Appends new line character(s) to the current line.
     *
     * @return This CsvWriter.
     * @throws IOException if a write error occurs
     */
    public CsvWriter endLine() throws IOException {
        writer.write(lineDelimiter, 0, lineDelimiter.length());
        isNewline = true;
        if (earlyFlush) {
            writer.flushBuffer();
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
