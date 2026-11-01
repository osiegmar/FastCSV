package de.siegmar.fastcsv.writer;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
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

/// This is the main class for writing CSV data.
///
/// This class is not thread-safe and must not be shared between threads.
///
/// Example use:
/// ```
/// try (CsvWriter csv = CsvWriter.builder().build(Path.of("output.csv"))) {
///     csv.writeRecord("Hello", "world");
/// }
/// ```
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
        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.writer = writer;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.quoteStrategy = quoteStrategy;
        this.lineDelimiter = lineDelimiter;

        emptyFieldValue = new char[] {quoteCharacter, quoteCharacter};
        lineDelimiterChars = lineDelimiter.toString().toCharArray();
    }

    /*
     * Add every control character rule here, not to the constructor body: CsvWriterBuilder checks
     * these before it opens a file, because opening truncates it under the default open options and
     * a later failure cannot undo that.
     */
    private static void assertFields(final char fieldSeparator, final char quoteCharacter,
                                     final char commentCharacter) {
        Preconditions.checkArgument(!Util.isNewline(fieldSeparator), "fieldSeparator must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.containsDupe(fieldSeparator, quoteCharacter, commentCharacter), () ->
            "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)".formatted(
                fieldSeparator, quoteCharacter, commentCharacter));
    }

    /// Creates a [CsvWriterBuilder] instance used to configure and create instances of
    /// this class.
    ///
    /// @return CsvWriterBuilder instance with default settings.
    public static CsvWriterBuilder builder() {
        return new CsvWriterBuilder();
    }

    /// Writes a record: its fields, followed by a line break.
    ///
    /// @param values the field contents to write (`null` values are handled as empty strings, if
    ///               not configured otherwise ([QuoteStrategies#EMPTY])).
    /// @return This CsvWriter.
    /// @throws UncheckedIOException if a write-error occurs
    /// @throws IllegalStateException if a record is already started (by calling [#writeRecord()]) and not ended
    /// @see #writeRecord(String...)
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

    /// Writes a record: its fields, followed by a line break.
    ///
    /// @param values the field contents to write (`null` values are handled as empty strings, if
    ///               not configured otherwise ([QuoteStrategies#EMPTY]))
    /// @return This CsvWriter.
    /// @throws UncheckedIOException if a write-error occurs
    /// @throws IllegalStateException if a record is already started (by calling [#writeRecord()]) and not ended
    /// @see #writeRecord(Iterable)
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

    /// Starts a new record.
    ///
    /// This method is used to write a record field by field. The record is ended by calling
    /// [#endRecord()].
    ///
    /// @return CsvWriterRecord instance to write fields to.
    /// @throws IllegalStateException if a record is already started
    /// @see #writeRecord(String...)
    /// @see #writeRecord(Iterable)
    public CsvWriterRecord writeRecord() {
        validateNoOpenRecord();
        openRecordWriter = true;
        return new CsvWriterRecord();
    }

    private void validateNoOpenRecord() {
        if (openRecordWriter) {
            throw new IllegalStateException("Record already started, call endRecord() on CsvWriterRecord first");
        }
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    private void writeInternal(final String value, final int fieldIdx) throws IOException {
        if (fieldIdx > 0) {
            writer.write(fieldSeparator);
        }

        if (value == null) {
            if (quoteStrategy.quoteNull(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue, 0, emptyFieldValue.length);
            }
            return;
        }

        final int length = value.length();

        if (length == 0) {
            if (quoteStrategy.quoteEmpty(currentLineNo, fieldIdx)) {
                writer.write(emptyFieldValue, 0, emptyFieldValue.length);
            }
            return;
        }

        final boolean needsEscape = containsControlCharacter(value, fieldIdx, length);
        final boolean needsQuotes = needsEscape || quoteStrategy.quoteValue(currentLineNo, fieldIdx, value);

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
            // Write up to and including the quote character
            w.write(value, startPos, nextDelimPos - startPos + 1);
            w.write(quoteChar);
            startPos = nextDelimPos + 1;
            nextDelimPos = value.indexOf(quoteChar, startPos);
        }

        // Write the rest of the string
        w.write(value, startPos, value.length() - startPos);
    }

    /// Writes a comment, followed by a line break.
    ///
    /// Note that comments are not part of RFC 4180 and may not be supported by all readers.
    ///
    /// @param comment the comment text to write. The comment character
    ///                (configured by [CsvWriterBuilder#commentCharacter(char)]) is automatically prepended.
    ///                `null` or an empty string writes a comment consisting of the comment character only.
    ///                If the comment text contains line breaks (`CR`, `LF`, `CRLF`), a separate comment is
    ///                written for each line, each followed by the line break configured by
    ///                [CsvWriterBuilder#lineDelimiter(LineDelimiter)].
    /// @return This CsvWriter.
    /// @throws UncheckedIOException if a write-error occurs
    /// @throws IllegalStateException if a record is already started (by calling [#writeRecord()]) and not ended
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

    /// Closes the underlying writer. Any buffered data is flushed before closing.
    ///
    /// @throws IOException if an I/O error occurs
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /// Flushes any buffered data to the underlying writer.
    ///
    /// @throws IOException if an I/O error occurs
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

    /// This builder is used to create configured instances of [CsvWriter]. The default
    /// configuration of this class adheres with RFC 4180.
    ///
    /// - field separator: `,` (comma)
    /// - quote character: `"` (double quote)
    /// - comment character: `#` (hash/number)
    /// - quote strategy: [QuoteStrategies#REQUIRED]
    /// - line break: [LineDelimiter#CRLF]
    /// - buffer size: 8,192 bytes
    /// - auto flush: `false`
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class CsvWriterBuilder {

        private static final int DEFAULT_BUFFER_SIZE = 8192;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private char commentCharacter = '#';
        private QuoteStrategy quoteStrategy = QuoteStrategies.REQUIRED;
        private LineDelimiter lineDelimiter = LineDelimiter.CRLF;
        private int bufferSize = DEFAULT_BUFFER_SIZE;
        private boolean autoFlush;

        CsvWriterBuilder() {
        }

        /// Sets the character that is used to separate fields – default: `,` (comma).
        ///
        /// @param fieldSeparator the field separator character.
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvWriterBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /// Sets the quote character – default: `"` (double quote).
        ///
        /// Be aware that using characters other than the default double quote character goes against the RFC 4180
        /// standard.
        ///
        /// @param quoteCharacter the quote character.
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvWriterBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /// Sets the comment character – default: `#` (hash/number).
        ///
        /// @param commentCharacter the character that begins each comment.
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvWriterBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /// Sets the strategy that defines when optional quoting has to be performed
        /// (default: [QuoteStrategies#REQUIRED]).
        ///
        /// @param quoteStrategy the strategy that decides whether to quote a field when quoting is not required;
        ///                      must not be `null`.
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws NullPointerException if quoteStrategy is `null`
        /// @see QuoteStrategies
        public CsvWriterBuilder quoteStrategy(final QuoteStrategy quoteStrategy) {
            this.quoteStrategy = Objects.requireNonNull(quoteStrategy, "quoteStrategy must not be null");
            return this;
        }

        /// Sets the line break written after each record and comment (default: [LineDelimiter#CRLF]).
        ///
        /// @param lineDelimiter the line break to be written; must not be `null`.
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws NullPointerException if lineDelimiter is `null`
        public CsvWriterBuilder lineDelimiter(final LineDelimiter lineDelimiter) {
            this.lineDelimiter = Objects.requireNonNull(lineDelimiter, "lineDelimiter must not be null");
            return this;
        }

        /// Configures the size of the internal buffer.
        ///
        /// The default buffer size of 8,192 bytes usually does not need to be altered. One use-case is if you
        /// need many instances of a CsvWriter and need to optimize for instantiation time and memory footprint.
        ///
        /// A buffer size of 0 disables the buffer.
        ///
        /// This setting is ignored when using [#toConsole()] as console output is unbuffered.
        ///
        /// @param bufferSize the buffer size to be used (must be &ge; 0).
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if bufferSize is negative
        public CsvWriterBuilder bufferSize(final int bufferSize) {
            Preconditions.checkArgument(bufferSize >= 0, "buffer size must be >= 0");
            this.bufferSize = bufferSize;
            return this;
        }

        /// Configures whether data should be flushed after each record write operation.
        ///
        /// Obviously this comes with drastic performance implications but can be useful for debugging purposes.
        ///
        /// This setting is ignored when using [#toConsole()] as console output is always flushed.
        ///
        /// @param autoFlush whether the data should be flushed after each record write operation.
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvWriterBuilder autoFlush(final boolean autoFlush) {
            this.autoFlush = autoFlush;
            return this;
        }

        /// Constructs a [CsvWriter] for the specified OutputStream.
        ///
        /// See [#build(OutputStream, Charset)] for details. This is just a convenience method
        /// for calling it with `charset` set to [StandardCharsets#UTF_8].
        ///
        /// @param outputStream the OutputStream to write CSV data to.
        /// @return a new CsvWriter instance - never `null`. Remember to close it!
        /// @throws NullPointerException if outputStream is `null`
        /// @see #build(OutputStream, Charset)
        public CsvWriter build(final OutputStream outputStream) {
            return build(outputStream, StandardCharsets.UTF_8);
        }

        /// Constructs a [CsvWriter] for the specified OutputStream and character set.
        ///
        /// This build method wraps the given `outputStream` with an [OutputStreamWriter].
        /// Both this library's internal buffer and the used [OutputStreamWriter] cause deferred writes to the
        /// underlying stream.
        /// You typically do not need to wrap the given `outputStream` in a [java.io.BufferedOutputStream].
        /// This ensures good performance but also means that you **must call [#flush()] or [#close()]**
        /// to ensure that all data is written to the underlying `outputStream`!
        ///
        /// Use [#build(Path,Charset,OpenOption...)] for optimal performance when writing files!
        ///
        /// @param outputStream the OutputStream to write CSV data to.
        /// @param charset      the character set to be used for writing data to the output stream.
        /// @return a new CsvWriter instance - never `null`. Remember to close it!
        /// @throws NullPointerException if outputStream or charset is `null`
        /// @see #build(OutputStream)
        public CsvWriter build(final OutputStream outputStream, final Charset charset) {
            Objects.requireNonNull(outputStream, "outputStream must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return csvWriter(new OutputStreamWriter(outputStream, charset), bufferSize, autoFlush);
        }

        /// Constructs a [CsvWriter] for the specified Writer.
        ///
        /// This library uses built-in buffering, unless [#bufferSize(int)] is used to disable it.
        /// You typically do not need to wrap the given `writer` in an [java.io.BufferedWriter].
        /// This ensures good performance but also means that you **must call [#flush()] or [#close()]**
        /// to ensure that all data is written to the underlying `writer`!
        ///
        /// Use [#build(Path,Charset,OpenOption...)] for optimal performance when writing files!
        ///
        /// @param writer the Writer to use for writing CSV data.
        /// @return a new CsvWriter instance - never `null`. Remember to close it!
        /// @throws NullPointerException if writer is `null`
        public CsvWriter build(final Writer writer) {
            Objects.requireNonNull(writer, "writer must not be null");

            return csvWriter(writer, bufferSize, autoFlush);
        }

        /// Constructs a [CsvWriter] for the specified Path.
        ///
        /// See [#build(Path,Charset,OpenOption...)] for details. This is just a convenience method
        /// for calling it with `charset` set to [StandardCharsets#UTF_8].
        ///
        /// @param file        the file to write data to.
        /// @param openOptions options specifying how the file is opened.
        ///                    See [Files#newOutputStream(Path,OpenOption...)] for defaults.
        /// @return a new CsvWriter instance - never `null`. Remember to close it!
        /// @throws IOException          if a write-error occurs
        /// @throws NullPointerException if file or charset is `null`
        public CsvWriter build(final Path file, final OpenOption... openOptions)
            throws IOException {
            return build(file, StandardCharsets.UTF_8, openOptions);
        }

        /// Constructs a [CsvWriter] for the specified Path.
        ///
        /// @param file        the file to write data to.
        /// @param charset     the character set to be used for writing data to the file.
        /// @param openOptions options specifying how the file is opened.
        ///                    See [Files#newOutputStream(Path,OpenOption...)] for defaults.
        /// @return a new CsvWriter instance - never `null`. Remember to close it!
        /// @throws IOException          if a write-error occurs
        /// @throws NullPointerException if file or charset is `null`
        public CsvWriter build(final Path file, final Charset charset,
                               final OpenOption... openOptions)
            throws IOException {

            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            // Opening the file creates or truncates it unless the caller's openOptions say otherwise –
            // reject an unusable configuration while that is still undone.
            assertFields(fieldSeparator, quoteCharacter, commentCharacter);

            return buildOwning(Files.newOutputStream(file, openOptions), charset);
        }

        /// Like [#build(OutputStream, Charset)] but takes ownership of the stream: if writer
        /// construction fails, the stream is closed before the exception propagates. The caller never
        /// receives a handle it could close itself.
        ///
        /// This does not undo the file creation or truncation the open already performed – it only
        /// keeps the descriptor from leaking.
        ///
        /// Only for streams this library opened itself – streams passed in by the caller stay owned by
        /// the caller and must not be closed here.
        ///
        /// @param outputStream the library-opened stream to write CSV data to.
        /// @param charset      the character set to use.
        /// @return a new CsvWriter - never `null`.
        @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
        CsvWriter buildOwning(final OutputStream outputStream, final Charset charset) {
            try {
                return build(outputStream, charset);
            } catch (final RuntimeException | Error e) {
                // Catching broadly is the point: whatever construction throws, the stream must not
                // survive it. Control character rules are already checked before the file is opened,
                // but the encoder and the buffer are only built here.
                try {
                    outputStream.close();
                } catch (final Exception | Error closeException) {
                    // Mirrors try-with-resources: a failure to close never displaces the failure that
                    // caused the close, whether it is checked or not.
                    e.addSuppressed(closeException);
                }
                throw e;
            }
        }

        /// Convenience method to write to the console (standard output).
        ///
        /// Settings [#bufferSize(int)] and [#autoFlush(boolean)] are ignored.
        /// Data is directly written to standard output and flushed after each record.
        ///
        /// Example use:
        /// ```
        /// CsvWriter.builder().toConsole()
        ///     .writeRecord("Hello", "world");
        /// ```
        ///
        /// @return a new CsvWriter instance - never `null`.
        ///     Calls to [CsvWriter#close()] are ignored, standard out remains open.
        @SuppressWarnings("checkstyle:RegexpMultiline")
        public CsvWriter toConsole() {
            final Writer writer = new NoCloseWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
            return csvWriter(writer, 0, true);
        }

        private CsvWriter csvWriter(final Writer writer, final int bufferSize,
                                    final boolean autoFlushWriter) {
            return new CsvWriter(wrapWriter(writer, bufferSize, autoFlushWriter),
                fieldSeparator, quoteCharacter, commentCharacter, quoteStrategy, lineDelimiter);
        }

        private static Writable wrapWriter(final Writer writer, final int bufferSize, final boolean autoFlushWriter) {
            if (bufferSize == 0) {
                return new UnbufferedWriter(writer, autoFlushWriter);
            }
            return autoFlushWriter
                ? new AutoflushingFastBufferedWriter(writer, bufferSize)
                : new FastBufferedWriter(writer, bufferSize);
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

    /// This class is used to write a record field by field.
    ///
    /// Obtain an instance via [CsvWriter#writeRecord()] (no-arg), write fields with [#writeField(String)],
    /// and complete the record with [#endRecord()].
    ///
    /// Example use:
    /// ```
    /// csv.writeRecord()
    ///     .writeField("field1")
    ///     .writeField("field2")
    ///     .endRecord();
    /// ```
    ///
    /// This class is not thread-safe.
    public final class CsvWriterRecord {

        private int fieldIdx;

        private CsvWriterRecord() {
        }

        /// Writes a field to the current record.
        /// @param value the field content
        /// @return this CsvWriterRecord instance
        /// @throws UncheckedIOException if a write-error occurs
        public CsvWriterRecord writeField(final String value) {
            try {
                writeInternal(value, fieldIdx++);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        /// Ends the current record.
        /// @return the enclosing CsvWriter instance
        /// @throws UncheckedIOException if a write-error occurs
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
