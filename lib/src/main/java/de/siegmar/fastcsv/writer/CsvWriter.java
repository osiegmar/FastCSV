package de.siegmar.fastcsv.writer;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;
import static de.siegmar.fastcsv.util.Util.containsDupe;

import java.io.Closeable;
import java.io.Flushable;
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
public final class CsvWriter implements Closeable, Flushable {

    private final Writable writer;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final char commentCharacter;
    private final QuoteStrategy quoteStrategy;
    private final LineDelimiter lineDelimiter;
    private int currentLineNo = 1;
    private final char[] lineDelimiterChars;
    private final char[] emptyFieldValue;
    private boolean openRecordWriter;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvWriter(final Writable writer, final char fieldSeparator, final char quoteCharacter,
              final char commentCharacter, final QuoteStrategy quoteStrategy, final LineDelimiter lineDelimiter) {
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
     *               not configured otherwise ({@link QuoteStrategies#EMPTY})).
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write-error occurs
     * @throws IllegalStateException if a record is already started (by calling {@link #writeRecord()}) and not ended
     * @see #writeRecord(String...)
     */
    public CsvWriter writeRecord(final Iterable<String> values) {
        validateNoOpenRecord();
        try {
            int fieldIdx = 0;
            for (final String value : values) {
                writeInternal(value, fieldIdx++);
            }
            return endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to write ({@code null} values are handled as empty strings, if
     *               not configured otherwise ({@link QuoteStrategies#EMPTY}))
     * @return This CsvWriter.
     * @throws UncheckedIOException if a write-error occurs
     * @throws IllegalStateException if a record is already started (by calling {@link #writeRecord()}) and not ended
     * @see #writeRecord(Iterable)
     */
    public CsvWriter writeRecord(final String... values) {
        validateNoOpenRecord();
        try {
            for (int i = 0; i < values.length; i++) {
                writeInternal(values[i], i);
            }
            return endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Starts a new record.
     * <p>
     * This method is used to write a record field by field. The record is ended by calling
     * {@link CsvWriterRecord#endRecord()}.
     *
     * @return CsvWriterRecord instance to write fields to.
     * @throws IllegalStateException if a record is already started
     * @see #writeRecord(String...)
     * @see #writeRecord(Iterable)
     */
    public CsvWriterRecord writeRecord() {
        validateNoOpenRecord();
        openRecordWriter = true;
        return new CsvWriterRecord();
    }

    private void validateNoOpenRecord() {
        if (openRecordWriter) {
            throw new IllegalStateException("Record already started, call end() on CsvWriterRecord first");
        }
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    private void writeInternal(final String value, final int fieldIdx) throws IOException {
        if (fieldIdx > 0) {
            writer.write(fieldSeparator);
        }

        if (value == null) {
            if (quoteStrategy != null && quoteStrategy.quoteNull(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue, 0, emptyFieldValue.length);
            }
            return;
        }

        final int length = value.length();

        if (length == 0) {
            if (quoteStrategy != null && quoteStrategy.quoteEmpty(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue, 0, emptyFieldValue.length);
            }
            return;
        }

        final boolean needsEscape = containsControlCharacter(value, fieldIdx, length);
        final boolean needsQuotes = needsEscape
            || quoteStrategy != null && quoteStrategy.quoteNonEmpty(currentLineNo, fieldIdx, value);

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }

        if (needsEscape) {
            writeEscaped(writer, value, quoteCharacter);
        } else {
            writer.write(value, 0, length);
        }

        if (needsQuotes) {
            writer.write(quoteCharacter);
        }
    }

    @SuppressWarnings({
        "checkstyle:BooleanExpressionComplexity",
        "checkstyle:ReturnCount",
        "checkstyle:MagicNumber",
        "PMD.AvoidLiteralsInIfCondition"
    })
    private boolean containsControlCharacter(final String value, final int fieldIdx, final int length) {
        if (fieldIdx == 0 && value.charAt(0) == commentCharacter) {
            return true;
        }

        // For longer values, indexOf is faster than iterating over the string
        if (length > 20) {
            return value.indexOf(quoteCharacter) != -1
                || value.indexOf(fieldSeparator) != -1
                || value.indexOf(LF) != -1
                || value.indexOf(CR) != -1;
        }

        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c == quoteCharacter || c == fieldSeparator || c == LF || c == CR) {
                return true;
            }
        }

        return false;
    }

    private static void writeEscaped(final Writable w, final String value, final char quoteChar)
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
     * @throws UncheckedIOException if a write-error occurs
     * @throws IllegalStateException if a record is already started (by calling {@link #writeRecord()}) and not ended
     */
    public CsvWriter writeComment(final String comment) {
        validateNoOpenRecord();
        try {
            writer.write(commentCharacter);
            if (comment != null && !comment.isEmpty()) {
                writeCommentInternal(comment);
            }
            return endRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private CsvWriter endRecord() throws IOException {
        ++currentLineNo;
        writer.write(lineDelimiterChars, 0, lineDelimiterChars.length);
        writer.endRecord();

        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
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
     * configuration of this class adheres with RFC 4180.
     * <ul>
     *     <li>field separator: {@code ,} (comma)</li>
     *     <li>quote character: {@code "} (double quote)</li>
     *     <li>comment character: {@code #} (hash/number)</li>
     *     <li>quote strategy: {@code null} (only required quoting)</li>
     *     <li>line delimiter: {@link LineDelimiter#CRLF}</li>
     *     <li>buffer size: 8,192 bytes</li>
     *     <li>auto flush: {@code false}</li>
     * </ul>
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
        private boolean autoFlush;

        CsvWriterBuilder() {
        }

        /**
         * Sets the character that is used to separate fields – default: {@code ,} (comma).
         *
         * @param fieldSeparator the field separator character.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /**
         * Sets the character used to quote values – default: {@code "} (double quote).
         * <p>
         * Be aware that using characters other than the default double quote character goes against the RFC 4180
         * standard.
         *
         * @param quoteCharacter the character for enclosing fields.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the character used to prepend commented lines – default: {@code #} (hash/number).
         *
         * @param commentCharacter the character for prepending commented lines.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines when optional quoting has to be performed – default: none.
         *
         * @param quoteStrategy the strategy when fields should be enclosed using the {@code quoteCharacter},
         *                      even if not strictly required.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder quoteStrategy(final QuoteStrategy quoteStrategy) {
            this.quoteStrategy = quoteStrategy;
            return this;
        }

        /**
         * Sets the delimiter used to separate lines (default: {@link LineDelimiter#CRLF}).
         *
         * @param lineDelimiter the line delimiter to be used.
         * @return This updated object, allowing additional method calls to be chained together.
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
         * <p>
         * This setting is ignored when using {@link #toConsole()} as console output is unbuffered.
         *
         * @param bufferSize the buffer size to be used (must be &ge; 0).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder bufferSize(final int bufferSize) {
            Preconditions.checkArgument(bufferSize >= 0, "buffer size must be >= 0");
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Configures whether data should be flushed after each record write operation.
         * <p>
         * Obviously this comes with drastic performance implications but can be useful for debugging purposes.
         * <p>
         * This setting is ignored when using {@link #toConsole()} as console output is always flushed.
         *
         * @param autoFlush whether the data should be flushed after each record write operation.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvWriterBuilder autoFlush(final boolean autoFlush) {
            this.autoFlush = autoFlush;
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

            return csvWriter(writer, bufferSize, true, autoFlush);
        }

        /**
         * Constructs a {@link CsvWriter} for the specified Path.
         *
         * @param file        the file to write data to.
         * @param openOptions options specifying how the file is opened.
         *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
         * @return a new CsvWriter instance - never {@code null}. Remember to close it!
         * @throws IOException          if a write-error occurs
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
         * @return a new CsvWriter instance - never {@code null}. Remember to close it!
         * @throws IOException          if a write-error occurs
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvWriter build(final Path file, final Charset charset,
                               final OpenOption... openOptions)
            throws IOException {

            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return csvWriter(new OutputStreamWriter(Files.newOutputStream(file, openOptions),
                charset), bufferSize, false, autoFlush);
        }

        /**
         * Convenience method to write to the console (standard output).
         * <p>
         * Settings {@link #bufferSize(int)} and {@link #autoFlush(boolean)} are ignored.
         * Data is directly written to standard output and flushed after each record.
         * <p>
         * Example use:
         * <p>
         * {@snippet :
         * CsvWriter.builder().toConsole()
         *     .writeRecord("Hello", "world");
         *}
         *
         * @return a new CsvWriter instance - never {@code null}.
         *     Calls to {@link #close()} are ignored, standard out remains open.
         */
        @SuppressWarnings("checkstyle:RegexpMultiline")
        public CsvWriter toConsole() {
            final Writer writer = new NoCloseWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
            return csvWriter(writer, 0, false, true);
        }

        private CsvWriter csvWriter(final Writer writer, final int bufferSize,
                                    final boolean autoFlushBuffer, final boolean autoFlushWriter) {
            final Writable writable = bufferSize > 0
                ? new FastBufferedWriter(writer, bufferSize, autoFlushBuffer, autoFlushWriter)
                : new UnbufferedWriter(writer, autoFlushWriter);

            return new CsvWriter(writable,
                fieldSeparator, quoteCharacter, commentCharacter, quoteStrategy, lineDelimiter);
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
                .add("autoFlush=" + autoFlush)
                .toString();
        }

    }

    /**
     * This class is used to write a record field by field.
     * <p>
     * The record is ended by calling {@link CsvWriterRecord#endRecord()}.
     */
    public final class CsvWriterRecord {

        private int fieldIdx;

        private CsvWriterRecord() {
        }

        /**
         * Writes a field to the current record.
         * @param value the field value
         * @return this CsvWriterRecord instance
         * @throws UncheckedIOException if a write-error occurs
         */
        public CsvWriterRecord writeField(final String value) {
            try {
                writeInternal(value, fieldIdx++);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        /**
         * Ends the current record.
         * @return the enclosing CsvWriter instance
         * @throws UncheckedIOException if a write-error occurs
         */
        public CsvWriter endRecord() {
            openRecordWriter = false;
            try {
                return CsvWriter.this.endRecord();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

}
