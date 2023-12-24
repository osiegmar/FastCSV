package de.siegmar.fastcsv.writer;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;
import static de.siegmar.fastcsv.util.Util.containsDupe;

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

import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/**
 * This is the main class for writing CSV data.
 * <p>
 * Example use:
 * {@snippet :
 * try (CsvWriter csv = CsvWriter.builder().build(file)) {
 *     csv.writeRecord("Hello", "world");
 * }
 *}
 */
@SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity"})
public final class CsvWriter implements Closeable {

    private final Writer writer;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final char commentCharacter;
    private final QuoteStrategy quoteStrategy;
    private final LineDelimiter lineDelimiter;
    private final boolean flushWriter;
    private int currentLineNo = 1;
    private final char[] lineDelimiterChars;
    private final char[] emptyFieldValue;

    CsvWriter(final Writer writer, final char fieldSeparator, final char quoteCharacter,
              final char commentCharacter, final QuoteStrategy quoteStrategy, final LineDelimiter lineDelimiter,
              final boolean flushWriter) {
        Preconditions.checkArgument(!Util.isNewline(fieldSeparator), "fieldSeparator must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!containsDupe(fieldSeparator, quoteCharacter, commentCharacter),
            "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
                fieldSeparator, quoteCharacter, commentCharacter);

        this.writer = writer;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.quoteStrategy = quoteStrategy;
        this.lineDelimiter = Objects.requireNonNull(lineDelimiter);
        this.flushWriter = flushWriter;

        emptyFieldValue = new char[] {quoteCharacter, quoteCharacter};
        lineDelimiterChars = lineDelimiter.toString().toCharArray();
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
     * @see #writeRecord(String...)
     */
    public CsvWriter writeRecord(final Iterable<String> values) {
        try {
            int fieldIdx = 0;
            for (final String value : values) {
                if (fieldIdx > 0) {
                    writer.write(fieldSeparator);
                }
                writeInternal(value, fieldIdx++);
            }
            endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    /**
     * Writes a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to write ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategy#EMPTY}))
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write error occurs
     * @see #writeRecord(Iterable)
     */
    public CsvWriter writeRecord(final String... values) {
        try {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    writer.write(fieldSeparator);
                }
                writeInternal(values[i], i);
            }
            endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    private void writeInternal(final String value, final int fieldIdx) throws IOException {
        if (value == null) {
            if (quoteStrategy != null && quoteStrategy.quoteNull(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue);
            }
            return;
        }

        final int length = value.length();

        if (length == 0) {
            if (quoteStrategy != null && quoteStrategy.quoteEmpty(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue);
            }
            return;
        }

        final boolean hasDelimiters = hasDelimiters(value, fieldIdx, length);
        final boolean needsQuotes = hasDelimiters
            || quoteStrategy != null && quoteStrategy.quoteNonEmpty(currentLineNo, fieldIdx, value);

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }

        if (hasDelimiters) {
            writeEscaped(writer, value, quoteCharacter);
        } else {
            writer.write(value, 0, length);
        }

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    private boolean hasDelimiters(final String value, final int fieldIdx, final int length) {
        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c == quoteCharacter || c == fieldSeparator || c == LF || c == CR
                || c == commentCharacter && fieldIdx == 0 && i == 0) {
                return true;
            }
        }

        return false;
    }

    private static void writeEscaped(final Writer w, final String value, final char quoteChar)
        throws IOException {

        int startPos = 0;
        int nextDelimPos = value.indexOf(quoteChar, startPos);

        while (nextDelimPos != -1) {
            // Write up to and including the delimiter
            w.write(value, startPos, nextDelimPos - startPos + 1);
            w.write(quoteChar);
            startPos = nextDelimPos + 1;
            nextDelimPos = value.indexOf(quoteChar, startPos);
        }

        // Write the rest of the string
        w.write(value, startPos, value.length() - startPos);
    }

    /**
     * Writes a comment line and new line character(s) at the end.
     * <p>
     * Note that comments are not part of the CSV standard and may not be supported by all readers.
     *
     * @param comment the comment to write. The comment character
     *                (configured by {@link CsvWriterBuilder#commentCharacter(char)}) is automatically prepended.
     *                Empty or {@code null} values results in a line only consisting of the comment character.
     *                If the argument {@code comment} contains line break characters (CR, LF), multiple comment lines
     *                will be written, terminated with the line break character configured by
     *                {@link CsvWriterBuilder#lineDelimiter(LineDelimiter)}.
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write error occurs
     */
    public CsvWriter writeComment(final String comment) {
        try {
            writer.write(commentCharacter);
            if (comment != null && !comment.isEmpty()) {
                writeCommentInternal(comment);
            }
            endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    private void writeCommentInternal(final String comment) throws IOException {
        final int length = comment.length();

        int startPos = 0;
        int lastChar = 0;
        for (int i = 0; i < length; i++) {
            final char c = comment.charAt(i);
            if (c == CR) {
                writeFragment(comment, i, startPos);
                startPos = i + 1;
            } else if (c == LF) {
                if (lastChar != CR) {
                    writeFragment(comment, i, startPos);
                }
                startPos = i + 1;
            }

            lastChar = c;
        }

        if (length > startPos) {
            writer.write(comment, startPos, length - startPos);
        }
    }

    private void writeFragment(final String comment, final int i, final int startPos) throws IOException {
        if (i > startPos) {
            writer.write(comment, startPos, i - startPos);
        }
        writer.write(lineDelimiterChars, 0, lineDelimiterChars.length);
        writer.write(commentCharacter);
    }

    private void endRecord() throws IOException {
        ++currentLineNo;
        writer.write(lineDelimiterChars, 0, lineDelimiterChars.length);
        if (flushWriter) {
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
        private QuoteStrategy quoteStrategy;
        private LineDelimiter lineDelimiter = LineDelimiter.CRLF;
        private int bufferSize = DEFAULT_BUFFER_SIZE;

        CsvWriterBuilder() {
        }

        /**
         * Sets the character that is used to separate fields (default: ',' - comma).
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
         * Sets the strategy that defines when optional quoting has to be performed (default: none).
         *
         * @param quoteStrategy the strategy when fields should be enclosed using the {@code quoteCharacter},
         *                      even if not strictly required.
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
            Preconditions.checkArgument(bufferSize >= 0, "buffer size must be >= 0");
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Writer.
         * <p>
         * This library uses built-in buffering (unless {@link #bufferSize(int)} is used to disable it) but writes
         * its internal buffer to the given {@code writer} at the end of every record write operation. Therefore,
         * you probably want to pass in a {@link java.io.BufferedWriter} to retain good performance.
         * Use {@link #build(Path, Charset, OpenOption...)} for optimal performance when writing files!
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
         * @param file        the file to write data to.
         * @param openOptions options specifying how the file is opened.
         *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
         * @return a new CsvWriter instance - never {@code null}. Don't forget to close it!
         * @throws IOException          if a write error occurs
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvWriter build(final Path file, final OpenOption... openOptions)
            throws IOException {
            return build(file, StandardCharsets.UTF_8, openOptions);
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Path.
         *
         * @param file        the file to write data to.
         * @param charset     the character set to be used for writing data to the file.
         * @param openOptions options specifying how the file is opened.
         *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
         * @return a new CsvWriter instance - never {@code null}. Don't forget to close it!
         * @throws IOException          if a write error occurs
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvWriter build(final Path file, final Charset charset,
                               final OpenOption... openOptions)
            throws IOException {

            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return newWriter(new OutputStreamWriter(Files.newOutputStream(file, openOptions),
                charset), false);
        }

        private CsvWriter newWriter(final Writer writer, final boolean flushWriter) {
            if (bufferSize > 0) {
                return new CsvWriter(new FastBufferedWriter(writer, bufferSize), fieldSeparator, quoteCharacter,
                    commentCharacter, quoteStrategy, lineDelimiter, flushWriter);
            }

            return new CsvWriter(writer, fieldSeparator, quoteCharacter,
                commentCharacter, quoteStrategy, lineDelimiter, false);
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
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            if (pos + len >= buf.length) {
                flush();
                if (len >= buf.length) {
                    writer.write(cbuf, off, len);
                    return;
                }
            }

            System.arraycopy(cbuf, off, buf, pos, len);
            pos += len;
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            if (pos + len >= buf.length) {
                flush();
                if (len >= buf.length) {
                    writer.write(str, off, len);
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
