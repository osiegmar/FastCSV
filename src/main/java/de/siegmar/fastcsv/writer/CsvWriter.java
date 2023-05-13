package de.siegmar.fastcsv.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * This is the main class for writing CSV data.
 * <p>
 * Example use:
 * <pre>{@code
 * try (CsvWriter csv = CsvWriter.builder().build(path)) {
 *     csv.writeRow("Hello", "world");
 * }
 * }</pre>
 */
@SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity"})
public final class CsvWriter implements Closeable {

    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Writer writer;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final char commentCharacter;
    private final QuoteStrategy quoteStrategy;
    private final String lineDelimiter;
    private final boolean syncWriter;

    CsvWriter(final Writer writer, final char fieldSeparator, final char quoteCharacter,
              final char commentCharacter, final QuoteStrategy quoteStrategy, final LineDelimiter lineDelimiter,
              final boolean syncWriter) {
        if (fieldSeparator == CR || fieldSeparator == LF) {
            throw new IllegalArgumentException("fieldSeparator must not be a newline char");
        }
        if (quoteCharacter == CR || quoteCharacter == LF) {
            throw new IllegalArgumentException("quoteCharacter must not be a newline char");
        }
        if (commentCharacter == CR || commentCharacter == LF) {
            throw new IllegalArgumentException("commentCharacter must not be a newline char");
        }
        if (!allDiffers(fieldSeparator, quoteCharacter, commentCharacter)) {
            throw new IllegalArgumentException(String.format("Control characters must differ"
                    + " (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
                fieldSeparator, quoteCharacter, commentCharacter));
        }

        this.writer = writer;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.quoteStrategy = Objects.requireNonNull(quoteStrategy);
        this.lineDelimiter = Objects.requireNonNull(lineDelimiter).toString();
        this.syncWriter = syncWriter;
    }

    private boolean allDiffers(final char... chars) {
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == chars[i + 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a {@link CsvWriterBuilder} instance used to configure and create instances of
     * this class.
     *
     * @return CsvWriterBuilder instance with default settings.
     */
    public static CsvWriterBuilder builder() {
        return new CsvWriterBuilder();
    }

    /**
     * Writes a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to write ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY})).
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write error occurs
     * @see #writeRow(String...)
     */
    public CsvWriter writeRow(final Iterable<String> values) {
        try {
            boolean firstField = true;
            for (final String value : values) {
                if (!firstField) {
                    writer.write(fieldSeparator);
                }
                writeInternal(value, firstField);
                firstField = false;
            }
            endRow();
            return this;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to write ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY}))
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write error occurs
     * @see #writeRow(Iterable)
     */
    public CsvWriter writeRow(final String... values) {
        try {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    writer.write(fieldSeparator);
                }
                writeInternal(values[i], i == 0);
            }
            endRow();
            return this;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    private void writeInternal(final String value, final boolean firstField) throws IOException {
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
        boolean needsQuotes = quoteStrategy == QuoteStrategy.ALWAYS || quoteStrategy == QuoteStrategy.NON_EMPTY;
        int nextDelimPos = -1;

        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c == quoteCharacter) {
                needsQuotes = true;
                nextDelimPos = i;
                break;
            }
            if (!needsQuotes && (c == fieldSeparator || c == LF || c == CR
                || firstField && i == 0 && c == commentCharacter)) {
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

    @SuppressWarnings({
        "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment",
        "PMD.AvoidReassigningParameters"
    })
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
     * Writes a comment line and new line character(s) at the end.
     *
     * @param comment the comment to write. The comment character
     *                (configured by {@link CsvWriterBuilder#commentCharacter(char)}) is automatically prepended.
     *                Empty or {@code null} values results in a line only consisting of the comment character.
     *                If the argument {@code comment} contains line break characters (CR, LF), multiple comment lines
     *                will be written, terminated with the line break character configured by
     *                {@link CsvWriterBuilder#lineDelimiter(LineDelimiter)}.
     *
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write error occurs
     */
    public CsvWriter writeComment(final String comment) {
        try {
            writer.write(commentCharacter);
            if (comment != null && !comment.isEmpty()) {
                writeCommentInternal(comment);
            }
            endRow();
            return this;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeCommentInternal(final String comment) throws IOException {
        final int length = comment.length();

        int startPos = 0;
        boolean lastCharWasCR = false;
        for (int i = 0; i < comment.length(); i++) {
            final char c = comment.charAt(i);
            if (c == CR) {
                final int len = i - startPos;
                writer.write(comment, startPos, len);
                writer.write(lineDelimiter, 0, lineDelimiter.length());
                writer.write(commentCharacter);
                startPos += len + 1;
                lastCharWasCR = true;
            } else if (c == LF) {
                if (lastCharWasCR) {
                    lastCharWasCR = false;
                    startPos++;
                } else {
                    final int len = i - startPos;
                    writer.write(comment, startPos, len);
                    writer.write(lineDelimiter, 0, lineDelimiter.length());
                    writer.write(commentCharacter);
                    startPos += len + 1;
                }
            } else {
                lastCharWasCR = false;
            }
        }

        if (length > startPos) {
            writer.write(comment, startPos, length - startPos);
        }
    }

    private void endRow() throws IOException {
        writer.write(lineDelimiter, 0, lineDelimiter.length());
        if (syncWriter) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvWriter.class.getSimpleName() + "[", "]")
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentCharacter=" + commentCharacter)
            .add("quoteStrategy=" + quoteStrategy)
            .add("lineDelimiter='" + lineDelimiter + "'")
            .toString();
    }

    /**
     * This builder is used to create configured instances of {@link CsvWriter}. The default
     * configuration of this class complies with RFC 4180.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class CsvWriterBuilder {

        private static final int DEFAULT_BUFFER_SIZE = 8192;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private char commentCharacter = '#';
        private QuoteStrategy quoteStrategy = QuoteStrategy.REQUIRED;
        private LineDelimiter lineDelimiter = LineDelimiter.CRLF;
        private int bufferSize = DEFAULT_BUFFER_SIZE;

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
         * Sets the character that is used to prepend commented lines (default: '#' - hash/number).
         *
         * @param commentCharacter the character for prepending commented lines.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines when quoting has to be performed
         * (default: {@link QuoteStrategy#REQUIRED}).
         *
         * @param quoteStrategy the strategy when fields should be enclosed using the {@code quoteCharacter}.
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
         * Configures the size of the internal buffer.
         * <p>
         * The default buffer size of 8,192 bytes usually does not need to be altered. One use-case is if you
         * need many instances of a CsvWriter and need to optimize for instantiation time and memory footprint.
         * <p>
         * A buffer size of 0 disables the buffer.
         *
         * @param bufferSize the buffer size to be used (must be &ge; 0).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvWriterBuilder bufferSize(final int bufferSize) {
            if (bufferSize < 0) {
                throw new IllegalArgumentException("buffer size must be >= 0");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Writer.
         * <p>
         * This library uses built-in buffering but writes its internal buffer to the given
         * {@code writer} on every {@link CsvWriter#writeRow(String...)} or
         * {@link CsvWriter#writeRow(Iterable)} call. Therefore, you probably want to pass in a
         * {@link java.io.BufferedWriter} to retain good performance.
         * Use {@link #build(Path, Charset, OpenOption...)} for optimal performance when writing
         * files.
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
         * @param openOptions options specifying how the file is opened.
         *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
         * @return a new CsvWriter instance - never {@code null}. Don't forget to close it!
         * @throws IOException          if a write error occurs
         * @throws NullPointerException if path or charset is {@code null}
         */
        public CsvWriter build(final Path path, final OpenOption... openOptions)
            throws IOException {
            return build(path, StandardCharsets.UTF_8, openOptions);
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
            if (bufferSize > 0) {
                return new CsvWriter(new FastBufferedWriter(writer, bufferSize), fieldSeparator, quoteCharacter,
                    commentCharacter, quoteStrategy, lineDelimiter, syncWriter);
            }

            return new CsvWriter(writer, fieldSeparator, quoteCharacter, commentCharacter, quoteStrategy,
                lineDelimiter, false);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CsvWriterBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentCharacter=" + commentCharacter)
                .add("quoteStrategy=" + quoteStrategy)
                .add("lineDelimiter=" + lineDelimiter)
                .add("bufferSize=" + bufferSize)
                .toString();
        }

    }

    /**
     * Unsynchronized and thus high performance replacement for BufferedWriter.
     * <p>
     * This class is intended for internal use only.
     */
    static final class FastBufferedWriter extends Writer {

        private final Writer writer;
        private final char[] buf;
        private int pos;

        FastBufferedWriter(final Writer writer, final int bufferSize) {
            this.writer = writer;
            buf = new char[bufferSize];
        }

        @Override
        public void write(final int c) throws IOException {
            if (pos == buf.length) {
                flush();
            }
            buf[pos++] = (char) c;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            if (pos + len >= buf.length) {
                flush();
                if (len >= buf.length) {
                    final char[] tmp = new char[len];
                    str.getChars(off, off + len, tmp, 0);
                    writer.write(tmp, 0, len);
                    return;
                }
            }

            str.getChars(off, off + len, buf, pos);
            pos += len;
        }

        @Override
        public void flush() throws IOException {
            writer.write(buf, 0, pos);
            pos = 0;
        }

        @Override
        public void close() throws IOException {
            flush();
            writer.close();
        }

    }

}
