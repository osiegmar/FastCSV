package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.siegmar.fastcsv.util.Limits;
import de.siegmar.fastcsv.util.Preconditions;

/// This is the main class for reading CSV data.
///
/// The CSV records are read iteratively, regardless of whether the Iterable, the Iterator, or the Stream is used.
/// Once all records are read, the data is consumed. If you need to repeatedly read records, you should collect the
/// records in a List or another collection.
///
/// Example use:
/// ```
/// try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
///     for (CsvRecord csvRecord : csv) {
///         // ...
///     }
/// }
/// ```
///
/// Example for named records:
/// ```
/// try (CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(file)) {
///     for (NamedCsvRecord csvRecord : csv) {
///         // ...
///     }
/// }
/// ```
///
/// @param <T> the type of the CSV record.
public final class CsvReader<T> implements Iterable<T>, Closeable {

    private final CsvParser csvParser;
    private final CsvCallbackHandler<T> callbackHandler;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyLines;
    private final boolean ignoreDifferentFieldCount;
    private final CloseableIterator<T> csvRecordIterator = new CsvRecordIterator();

    private int firstRecordFieldCount = -1;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvReader(final CsvParser csvParser, final CsvCallbackHandler<T> callbackHandler,
              final CommentStrategy commentStrategy, final boolean skipEmptyLines,
              final boolean ignoreDifferentFieldCount) {

        this.csvParser = csvParser;
        this.callbackHandler = callbackHandler;
        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;
    }

    /// Constructs a [CsvReaderBuilder] to configure and build instances of this class.
    ///
    /// @return a new [CsvReaderBuilder] instance.
    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    /// Skips the specified number of lines.
    ///
    /// The setting [CsvReaderBuilder#skipEmptyLines(boolean)] has no effect on this method.
    ///
    /// @param lineCount the number of lines to skip.
    /// @throws IllegalArgumentException if lineCount is negative.
    /// @throws UncheckedIOException     if an I/O error occurs.
    /// @throws CsvParseException        if not enough lines are available to skip.
    public void skipLines(final int lineCount) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount must be non-negative");
        }

        try {
            for (int i = 0; i < lineCount; i++) {
                if (!csvParser.skipLine(0)) {
                    throw new CsvParseException("Not enough lines to skip. Skipped only %d line(s).".formatted(i));
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Skip lines until the specified predicate matches.
    /// The line that matches the predicate is not skipped.
    ///
    /// The method returns the number of lines actually skipped.
    ///
    /// The setting [CsvReaderBuilder#skipEmptyLines(boolean)] has no effect on this method.
    ///
    /// @param predicate the predicate to match the lines.
    /// @param maxLines  the maximum number of lines to skip.
    /// @return the number of lines actually skipped.
    /// @throws NullPointerException if predicate is `null`.
    /// @throws IllegalArgumentException if maxLines is negative.
    /// @throws UncheckedIOException if an I/O error occurs.
    /// @throws CsvParseException if no matching line is found within the maximum limit of maxLines.
    public int skipLines(final Predicate<String> predicate, final int maxLines) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        if (maxLines < 0) {
            throw new IllegalArgumentException("maxLines must be non-negative");
        }

        if (maxLines == 0) {
            return 0;
        }

        try {
            for (int i = 0; i < maxLines; i++) {
                final String line = csvParser.peekLine();
                if (predicate.test(line)) {
                    return i;
                }

                if (!csvParser.skipLine(line.length())) {
                    throw new CsvParseException(
                        "No matching line found. Skipped %d line(s) before reaching end of data.".formatted(i));
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        throw new CsvParseException(
            "No matching line found within the maximum limit of %d lines.".formatted(maxLines));
    }

    /// {@return an iterator over elements of type [CsvRecord].}
    ///
    /// The returned iterator is not thread-safe.
    /// Remember to close the returned iterator when you're done.
    /// Alternatively, use [#stream()].
    ///
    /// This method is idempotent and can be called multiple times.
    ///
    /// @throws UncheckedIOException if an I/O error occurs.
    /// @throws CsvParseException    if any other problem occurs when parsing the CSV data.
    /// @see #stream()
    @Override
    public CloseableIterator<T> iterator() {
        return csvRecordIterator;
    }

    /// Constructs a [Spliterator] for splitting and traversing the elements of this reader.
    ///
    /// The returned spliterator is not thread-safe.
    /// Remember to invoke [#close()] when you're done.
    /// Alternatively, use [#stream()].
    ///
    /// This method is idempotent and can be called multiple times.
    ///
    /// @return a spliterator over the CSV records.
    /// @throws UncheckedIOException if an I/O error occurs.
    /// @throws CsvParseException    if any other problem occurs when parsing the CSV data.
    /// @see #stream()
    @Override
    public Spliterator<T> spliterator() {
        return new CsvSpliterator();
    }

    /// Constructs a new sequential `Stream` with this reader as its source.
    ///
    /// The returned stream is not thread-safe.
    /// Remember to close the returned stream when you're done.
    /// Closing the stream will also close this reader.
    ///
    /// This method can be called multiple times, although it creates a new stream each time.
    ///
    /// The stream is not reusable after it has been closed.
    ///
    /// @return a sequential `Stream` over the CSV records.
    /// @throws UncheckedIOException if an I/O error occurs.
    /// @throws CsvParseException    if any other problem occurs when parsing the CSV data.
    /// @see #iterator()
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false)
            .onClose(() -> {
                try {
                    close();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    @SuppressWarnings({
        "PMD.AvoidBranchingStatementAsLastInLoop",
        "PMD.AssignmentInOperand"
    })
    private T fetchRecord() throws IOException {
        while (csvParser.parse()) {
            final T csvRecord = processRecord();

            if (csvRecord != null) {
                return csvRecord;
            }
        }

        callbackHandler.terminate();

        return null;
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    private T processRecord() {
        final T csvRecord = callbackHandler.buildRecord();

        // handle consumed records (e.g., header for named records)
        if (csvRecord == null) {
            return null;
        }

        // handle comment lines
        if (callbackHandler.isComment()) {
            return commentStrategy == CommentStrategy.SKIP ? null : csvRecord;
        }

        // handle empty lines
        if (callbackHandler.isEmptyLine()) {
            return skipEmptyLines ? null : csvRecord;
        }

        // check field count consistency
        if (!ignoreDifferentFieldCount) {
            checkFieldCountConsistency(callbackHandler.getFieldCount());
        }

        return csvRecord;
    }

    private void checkFieldCountConsistency(final int fieldCount) {
        // check the field count consistency on every record
        if (firstRecordFieldCount == -1) {
            firstRecordFieldCount = fieldCount;
        } else if (fieldCount != firstRecordFieldCount) {
            throw new CsvParseException("Record %d has %d fields, but first record had %d fields"
                .formatted(csvParser.getStartingLineNumber(), fieldCount, firstRecordFieldCount));
        }
    }

    @Override
    public void close() throws IOException {
        csvParser.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvReader.class.getSimpleName() + "[", "]")
            .add("commentStrategy=" + commentStrategy)
            .add("skipEmptyLines=" + skipEmptyLines)
            .add("ignoreDifferentFieldCount=" + ignoreDifferentFieldCount)
            .toString();
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingThrowable"})
    private T fetch() {
        try {
            return fetchRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(buildExceptionMessage(), e);
        } catch (final Throwable t) {
            throw new CsvParseException(buildExceptionMessage(), t);
        }
    }

    private String buildExceptionMessage() {
        return (csvParser.getStartingLineNumber() == 1)
            ? "Exception when reading first record"
            : "Exception when reading record that started in line %d".formatted(csvParser.getStartingLineNumber());
    }

    private class CsvSpliterator implements Spliterator<T> {

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            final T t = fetch();
            if (t != null) {
                action.accept(t);
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL;
        }

    }

    private class CsvRecordIterator implements CloseableIterator<T> {

        private T fetchedRecord;
        private boolean fetched;

        @Override
        public boolean hasNext() {
            if (!fetched) {
                fetchedRecord = fetch();
                fetched = true;
            }
            return fetchedRecord != null;
        }

        @Override
        public T next() {
            if (!fetched) {
                fetchedRecord = fetch();
            }
            if (fetchedRecord == null) {
                throw new NoSuchElementException();
            }

            fetched = false;
            return fetchedRecord;
        }

        @Override
        public void close() throws IOException {
            CsvReader.this.close();
        }

    }

    /// This builder is used to create configured instances of [CsvReader]. The default
    /// configuration of this class adheres with RFC 4180:
    ///
    /// - Field separator: `,` (comma)
    /// - Quote character: `"` (double quotes)
    /// - Comment strategy: [CommentStrategy#NONE] (as RFC doesn't handle comments)
    /// - Comment character: `#` (hash) (in case comment strategy is enabled)
    /// - Skip empty lines: `true`
    /// - Ignore different field count: `true`
    /// - Accept characters after quotes: `true`
    /// - Detect BOM header: `false`
    /// - Max buffer size: {@value %,2d #DEFAULT_MAX_BUFFER_SIZE} characters
    ///
    /// The line delimiter (line-feed, carriage-return or the combination of both) is detected
    /// automatically and thus not configurable.
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class CsvReaderBuilder {

        private static final int DEFAULT_MAX_BUFFER_SIZE = 16 * 1024 * 1024;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean skipEmptyLines = true;
        private boolean ignoreDifferentFieldCount = true;
        private boolean acceptCharsAfterQuotes = true;
        private boolean detectBomHeader;
        @SuppressWarnings("removal")
        private int maxBufferSize = Math.min(DEFAULT_MAX_BUFFER_SIZE, Limits.MAX_FIELD_SIZE);

        private CsvReaderBuilder() {
        }

        /// Sets the `fieldSeparator` used when reading CSV data.
        ///
        /// @param fieldSeparator the field separator character (default: `,` - comma).
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /// Sets the `quoteCharacter` used when reading CSV data.
        ///
        /// @param quoteCharacter the character used to enclose fields
        ///                       (default: `"` - double quotes).
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /// Sets the strategy that defines how (and if) commented lines should be handled
        /// (default: [CommentStrategy#NONE] as comments are not defined in RFC 4180).
        ///
        /// If a comment strategy other than [CommentStrategy#NONE] is used, special parsing rules are
        /// applied for commented lines. FastCSV defines a comment as a line that starts with a comment character.
        /// No (whitespace) character is allowed before the comment character. Everything after the comment character
        /// until the end of the line is considered the comment value.
        ///
        /// @param commentStrategy the strategy for handling comments.
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @see #commentCharacter(char)
        public CsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
            this.commentStrategy = commentStrategy;
            return this;
        }

        /// Sets the `commentCharacter` used to comment lines.
        ///
        /// @param commentCharacter the character used to comment lines (default: `#` - hash)
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @see #commentStrategy(CommentStrategy)
        public CsvReaderBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /// Defines whether empty lines should be skipped when reading data.
        ///
        /// The default implementation interprets empty lines as lines that do not contain any data
        /// (no whitespace, no quotes, nothing).
        ///
        /// Commented lines are not considered empty lines. Use [#commentStrategy(CommentStrategy)] for handling
        /// commented lines.
        ///
        /// @param skipEmptyLines Whether empty lines should be skipped (default: `true`).
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder skipEmptyLines(final boolean skipEmptyLines) {
            this.skipEmptyLines = skipEmptyLines;
            return this;
        }

        /// Defines if an [CsvParseException] should be thrown if records do contain a
        /// different number of fields.
        ///
        /// @param ignoreDifferentFieldCount if exception should be suppressed, when CSV data contains
        ///                                  different field count (default: `true`).
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder ignoreDifferentFieldCount(final boolean ignoreDifferentFieldCount) {
            this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;
            return this;
        }

        /// Specifies whether the presence of characters between a closing quote and a field separator or
        /// the end of a line should be treated as an error or not.
        ///
        /// Example: `"a"b,"c"`
        ///
        /// If this is set to `true`, the value `ab` will be returned for the first field.
        ///
        /// If this is set to `false`, a [CsvParseException] will be thrown.
        ///
        /// @param acceptCharsAfterQuotes allow characters after quotes (default: `true`).
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder acceptCharsAfterQuotes(final boolean acceptCharsAfterQuotes) {
            this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
            return this;
        }

        /// Defines if an optional BOM (Byte order mark) header should be detected.
        ///
        /// **BOM detection only applies for [InputStream] and
        /// [Path] based data sources.
        /// It does not apply for [Reader] or [String] based data sources
        /// as they are already decoded.**
        ///
        /// Supported BOMs are: UTF-8, UTF-16LE, UTF-16BE, UTF-32LE, UTF-32BE.
        ///
        /// @param detectBomHeader if detection should be enabled (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together.
        public CsvReaderBuilder detectBomHeader(final boolean detectBomHeader) {
            this.detectBomHeader = detectBomHeader;
            return this;
        }

        /// Defines the maximum buffer size used when parsing data.
        ///
        /// The size of the internal buffer is automatically adjusted to the needs of the parser.
        /// To protect against out-of-memory errors, its maximum size is limited.
        ///
        /// The buffer is used for two purposes:
        ///   - Reading data from the underlying stream of data in chunks
        ///   - Storing the data of a single field before it is passed to the callback handler
        ///
        /// Set a larger value only if you expect to read fields larger than the default limit.
        /// In that case you probably **also need to adjust** the maximum field size of the callback handler.
        ///
        /// Set a smaller value if your runtime environment has not enough memory available for the default value.
        /// Setting values smaller than 16,384 characters will most likely lead to performance degradation.
        ///
        /// @param maxBufferSize the maximum buffer size in characters (default: {@value %,2d #DEFAULT_MAX_BUFFER_SIZE})
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if maxBufferSize is not positive
        public CsvReaderBuilder maxBufferSize(final int maxBufferSize) {
            Preconditions.checkArgument(maxBufferSize > 0, "maxBufferSize must be greater than 0");
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /// Constructs a new index-based [CsvReader] for the specified input stream.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// @param inputStream the input stream to read data from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if inputStream is `null`
        /// @see #ofCsvRecord(InputStream, Charset)
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream) {
            return build(CsvRecordHandler.of(), inputStream);
        }

        /// Constructs a new index-based [CsvReader] for the specified input stream and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream,Charset)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// @param inputStream the input stream to read data from.
        /// @param charset     the character set to use. If BOM header detection is enabled
        ///                    (via [#detectBomHeader(boolean)]), this acts as a default
        ///                    when no BOM header was found.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if inputStream or charset is `null`
        /// @see #ofCsvRecord(InputStream)
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream, final Charset charset) {
            return build(CsvRecordHandler.of(), inputStream, charset);
        }

        /// Constructs a new index-based [CsvReader] for the specified reader.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Reader)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param reader the data source to read from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if reader is `null`
        public CsvReader<CsvRecord> ofCsvRecord(final Reader reader) {
            return build(CsvRecordHandler.of(), reader);
        }

        /// Constructs a new index-based [CsvReader] for the specified String.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,String)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param data the data to read.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if data is `null`
        public CsvReader<CsvRecord> ofCsvRecord(final String data) {
            return build(CsvRecordHandler.of(), data);
        }

        /// Constructs a new index-based [CsvReader] for the specified file.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// @param file the file to read data from.
        /// @return a new CsvReader - never `null`. Don't forget to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file is `null`
        /// @see #ofCsvRecord(Path, Charset)
        public CsvReader<CsvRecord> ofCsvRecord(final Path file) throws IOException {
            return build(CsvRecordHandler.of(), file);
        }

        /// Constructs a new index-based [CsvReader] for the specified file and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path,Charset)] with
        /// [CsvRecordHandler] as callback handler.
        ///
        /// @param file    the file to read data from.
        /// @param charset the character set to use. If BOM header detection is enabled
        ///                (via [#detectBomHeader(boolean)]), this acts as a default
        ///                when no BOM header was found.
        /// @return a new CsvReader - never `null`. Don't forget to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file or charset is `null`
        /// @see #ofCsvRecord(Path)
        public CsvReader<CsvRecord> ofCsvRecord(final Path file, final Charset charset) throws IOException {
            return build(CsvRecordHandler.of(), file, charset);
        }

        /// Constructs a new name-based [CsvReader] for the specified input stream.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// @param inputStream the input stream to read data from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if reader is `null`
        /// @see #ofNamedCsvRecord(InputStream, Charset)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream) {
            return build(NamedCsvRecordHandler.of(), inputStream);
        }

        /// Constructs a new name-based [CsvReader] for the specified input stream and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream,Charset)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// @param inputStream the input stream to read data from.
        /// @param charset     the character set to use. If BOM header detection is enabled
        ///                    (via [#detectBomHeader(boolean)]), this acts as a default
        ///                    when no BOM header was found.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if file or charset is `null`
        /// @see #ofNamedCsvRecord(InputStream)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream, final Charset charset) {
            return build(NamedCsvRecordHandler.of(), inputStream, charset);
        }

        /// Constructs a new name-based [CsvReader] for the specified reader.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Reader)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param reader the data source to read from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if reader is `null`
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Reader reader) {
            return build(NamedCsvRecordHandler.of(), reader);
        }

        /// Constructs a new name-based [CsvReader] for the specified String.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,String)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param data the data to read.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if data is `null`
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final String data) {
            return build(NamedCsvRecordHandler.of(), data);
        }

        /// Constructs a new name-based [CsvReader] for the specified file.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// @param file the file to read data from.
        /// @return a new CsvReader - never `null`. Don't forget to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file is `null`
        /// @see #ofNamedCsvRecord(Path, Charset)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Path file) throws IOException {
            return build(NamedCsvRecordHandler.of(), file);
        }

        /// Constructs a new name-based [CsvReader] for the specified file and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path,Charset)] with
        /// [NamedCsvRecordHandler] as callback handler.
        ///
        /// @param file    the file to read data from.
        /// @param charset the character set to use. If BOM header detection is enabled
        ///                (via [#detectBomHeader(boolean)]), this acts as a default
        ///                when no BOM header was found.
        /// @return a new CsvReader - never `null`. Don't forget to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file or charset is `null`
        /// @see #ofNamedCsvRecord(Path)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Path file, final Charset charset)
            throws IOException {
            return build(NamedCsvRecordHandler.of(), file, charset);
        }

        /// Constructs a new callback-based [CsvReader] for the specified input stream.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream,Charset)].
        ///
        /// This library uses built-in buffering, so you do not need to pass in a buffered InputStream
        /// implementation such as [java.io.BufferedInputStream]. Performance may be even likely
        /// better if you do not.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// Use [#build(CsvCallbackHandler,Path)] for optimal performance when reading files.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param inputStream     the input stream to read data from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if callbackHandler or inputStream is `null`
        /// @see #build(CsvCallbackHandler, InputStream, Charset)
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final InputStream inputStream) {
            return build(callbackHandler, inputStream, StandardCharsets.UTF_8);
        }

        /// Constructs a new callback-based [CsvReader] for the specified input stream and character set.
        ///
        /// This library uses built-in buffering, so you do not need to pass in a buffered InputStream
        /// implementation such as [java.io.BufferedInputStream]. Performance may be even likely
        /// better if you do not.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, this method will immediately cause consumption of the
        /// input stream to read the BOM header and determine the character set.
        ///
        /// Use [#build(CsvCallbackHandler,Path,Charset)] for optimal performance when reading files.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param inputStream     the input stream to read data from.
        /// @param charset         the character set to use. If BOM header detection is enabled
        ///                        (via [#detectBomHeader(boolean)]), this acts as a default
        ///                        when no BOM header was found.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if callbackHandler, inputStream or charset is `null`
        /// @see #build(CsvCallbackHandler, InputStream)
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                      final InputStream inputStream, final Charset charset) {

            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(inputStream, "inputStream must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final Reader reader = detectBomHeader
                ? new BomInputStreamReader(inputStream, charset)
                : new InputStreamReader(inputStream, charset);

            return build(callbackHandler, reader);
        }

        /// Constructs a new callback-based [CsvReader] for the specified reader.
        ///
        /// This library uses built-in buffering, so you do not need to pass in a buffered Reader
        /// implementation such as [java.io.BufferedReader]. Performance may be even likely
        /// better if you do not.
        ///
        /// Use [#build(CsvCallbackHandler,Path)] for optimal performance when
        /// reading files and [#build(CsvCallbackHandler,String)] when reading Strings.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param reader          the data source to read from.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if callbackHandler or reader is `null`
        /// @throws IllegalArgumentException if argument validation fails.
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Reader reader) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(reader, "reader must not be null");

            final CsvParser csvParser = new CsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, acceptCharsAfterQuotes, callbackHandler, maxBufferSize, reader);

            return newReader(callbackHandler, csvParser);
        }

        /// Constructs a new callback-based [CsvReader] for the specified String.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param data            the data to read.
        /// @return a new CsvReader - never `null`.
        /// @throws NullPointerException if callbackHandler or data is `null`
        /// @throws IllegalArgumentException if argument validation fails.
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final String data) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(data, "data must not be null");

            final CsvParser csvParser = new CsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, acceptCharsAfterQuotes, callbackHandler, data);

            return newReader(callbackHandler, csvParser);
        }

        /// Constructs a new callback-based [CsvReader] for the specified file.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM header.
        /// Per default the character set is [StandardCharsets#UTF_8].
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param file            the file to read data from.
        /// @return a new CsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if callbackHandler or file is `null`
        /// @see #build(CsvCallbackHandler, Path, Charset)
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Path file)
            throws IOException {
            return build(callbackHandler, file, StandardCharsets.UTF_8);
        }

        /// Constructs a new callback-based [CsvReader] for the specified file and character set.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
        /// @param file            the file to read data from.
        /// @param charset         the character set to use. If BOM header detection is enabled
        ///                        (via [#detectBomHeader(boolean)]), this acts as a default
        ///                        when no BOM header was found.
        /// @return a new CsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if callbackHandler, file or charset is `null`
        /// @see #build(CsvCallbackHandler, Path)
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                      final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final Reader reader = detectBomHeader
                ? BomUtil.openReader(file, charset)
                : new InputStreamReader(Files.newInputStream(file), charset);

            return build(callbackHandler, reader);
        }

        private <T> CsvReader<T> newReader(final CsvCallbackHandler<T> callbackHandler, final CsvParser csvParser) {
            return new CsvReader<>(csvParser, callbackHandler,
                commentStrategy, skipEmptyLines, ignoreDifferentFieldCount);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CsvReaderBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentStrategy=" + commentStrategy)
                .add("commentCharacter=" + commentCharacter)
                .add("skipEmptyLines=" + skipEmptyLines)
                .add("ignoreDifferentFieldCount=" + ignoreDifferentFieldCount)
                .add("acceptCharsAfterQuotes=" + acceptCharsAfterQuotes)
                .add("detectBomHeader=" + detectBomHeader)
                .add("maxBufferSize=" + maxBufferSize)
                .toString();
        }

    }

}
