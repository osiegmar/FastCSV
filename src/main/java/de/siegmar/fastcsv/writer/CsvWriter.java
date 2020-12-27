package de.siegmar.fastcsv.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

import de.siegmar.fastcsv.util.Unchecker;

/**
 * This is the main class for writing CSV data.
 * <p>
 * Example use:
 * <pre>{@code
 * try (CsvWriter csv = CsvWriter.builder().build(path, StandardCharsets.UTF_8)) {
 *     csv.writeRow("Hello", "world");
 * }
 * }</pre>
 */
@SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity"})
public final class CsvWriter implements Closeable {

    private static final char CR = '\r';
    private static final char LF = '\n';

    private final CachingWriter writer;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final QuoteStrategy quoteStrategy;
    private final String lineDelimiter;
    private final boolean syncWriter;

    private boolean isNewline = true;

    CsvWriter(final Writer writer, final char fieldSeparator, final char quoteCharacter,
              final QuoteStrategy quoteStrategy, final LineDelimiter lineDelimiter,
              final boolean syncWriter) {

        if (fieldSeparator == CR || fieldSeparator == LF) {
            throw new IllegalArgumentException("fieldSeparator must not be a newline char");
        }
        if (quoteCharacter == CR || quoteCharacter == LF) {
            throw new IllegalArgumentException("quoteCharacter must not be a newline char");
        }
        if (fieldSeparator == quoteCharacter) {
            throw new IllegalArgumentException(String.format("Control characters must differ"
                    + " (fieldSeparator=%s, quoteCharacter=%s)",
                fieldSeparator, quoteCharacter));
        }

        this.writer = new CachingWriter(writer);
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.quoteStrategy = Objects.requireNonNull(quoteStrategy);
        this.lineDelimiter = Objects.requireNonNull(lineDelimiter).toString();
        this.syncWriter = syncWriter;
    }

    /**
     * Creates a {@link CsvWriterBuilder} instance used to configure and create instances of
     * this class.
     * @return CsvWriterBuilder instance with default settings.
     */
    public static CsvWriterBuilder builder() {
        return new CsvWriterBuilder();
    }

    private void writeInternal(final String value) {
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
    private void writeEscaped(final String value, final int length, int nextDelimPos) {
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
     */
    public CsvWriter writeRow(final Iterable<String> values) {
        for (final String value : values) {
            writeInternal(value);
        }
        endRow();
        return this;
    }

    /**
     * Appends a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to append ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY}))
     * @return This CsvWriter.
     */
    public CsvWriter writeRow(final String... values) {
        for (final String value : values) {
            writeInternal(value);
        }
        endRow();
        return this;
    }

    private void endRow() {
        writer.write(lineDelimiter, 0, lineDelimiter.length());
        isNewline = true;
        if (syncWriter) {
            writer.flushBuffer();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * This builder is used to create configured instances of {@link CsvWriter}. The default
     * configuration of this class complies with RFC 4180.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    public static final class CsvWriterBuilder {

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private QuoteStrategy quoteStrategy = QuoteStrategy.REQUIRED;
        private LineDelimiter lineDelimiter = LineDelimiter.CRLF;

        CsvWriterBuilder() {
        }

        /**
         * Sets the character that is used to separate columns (default: ',' - comma).
         *
         * @param fieldSeparator the field separator character.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /**
         * Sets the character that is used to quote values (default: '"' - double quotes).
         *
         * @param quoteCharacter the character for enclosing fields.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines when quoting has to be performed
         * (default: {@link QuoteStrategy#REQUIRED}).
         *
         * @param quoteStrategy the strategy when fields should be enclosed using the.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder quoteStrategy(final QuoteStrategy quoteStrategy) {
            this.quoteStrategy = quoteStrategy;
            return this;
        }

        /**
         * Sets the delimiter that is used to separate lines (default: {@link LineDelimiter#CRLF}).
         *
         * @param lineDelimiter the line delimiter to be used.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder lineDelimiter(final LineDelimiter lineDelimiter) {
            this.lineDelimiter = lineDelimiter;
            return this;
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Writer.
         * <p>
         * This library uses built-in buffering but writes its internal buffer to the given
         * {@code writer} on every {@link CsvWriter#writeRow(String...)} or
         * {@link CsvWriter#writeRow(Iterable)} call. Therefore you probably want to pass in a
         * {@link java.io.BufferedWriter} to retain good performance.
         * Use {@link #build(Path, Charset, OpenOption...)} for optimal performance.
         *
         * @param writer the Writer to use for writing CSV data.
         * @return a new CsvWriter instance - never {@code null}.
         * @throws NullPointerException if writer is {@code null}
         */
        public CsvWriter build(final Writer writer) {
            Objects.requireNonNull(writer, "writer must not be null");

            return newWriter(writer, true);
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Path.
         *
         * @param path        the path to write data to.
         * @param charset     the character set to be used for writing data to the file.
         * @param openOptions options specifying how the file is opened.
         *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
         * @return a new CsvWriter instance - never {@code null}. Don't forget to close it!
         * @throws IOException          if a write error occurs
         * @throws NullPointerException if path or charset is {@code null}
         */
        public CsvWriter build(final Path path, final Charset charset,
                               final OpenOption... openOptions)
            throws IOException {

            Objects.requireNonNull(path, "path must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return newWriter(new OutputStreamWriter(Files.newOutputStream(path, openOptions),
                charset), false);
        }

        private CsvWriter newWriter(final Writer writer, final boolean syncWriter) {
            return new CsvWriter(writer, fieldSeparator, quoteCharacter, quoteStrategy,
                lineDelimiter, syncWriter);
        }

    }

    /**
     * Unsynchronized and thus high performance replacement for BufferedWriter.
     * <p>
     * This class is intended for internal use only.
     */
    static final class CachingWriter {

        private static final int BUFFER_SIZE = 8192;

        private final Writer writer;
        private final char[] buf = new char[BUFFER_SIZE];
        private int pos;

        CachingWriter(final Writer writer) {
            this.writer = writer;
        }

        void write(final char c) {
            if (pos == BUFFER_SIZE) {
                flushBuffer();
            }
            buf[pos++] = c;
        }

        @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
        void write(final String str, final int off, final int len) {
            if (pos + len >= BUFFER_SIZE) {
                try {
                    internalFlushBuffer();
                    writer.write(str, off, len);
                } catch (final IOException e) {
                    Unchecker.uncheck(e);
                }
            } else {
                str.getChars(off, off + len, buf, pos);
                pos += len;
            }
        }

        private void flushBuffer() {
            try {
                internalFlushBuffer();
            } catch (final IOException e) {
                Unchecker.uncheck(e);
            }
        }

        private void internalFlushBuffer() throws IOException {
            writer.write(buf, 0, pos);
            pos = 0;
        }

        void close() {
            try {
                internalFlushBuffer();
                writer.close();
            } catch (final IOException e) {
                Unchecker.uncheck(e);
            }
        }

    }

}
