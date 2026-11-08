package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.EOFException;
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

import de.siegmar.fastcsv.util.Nullable;
import de.siegmar.fastcsv.util.Preconditions;

/// This is the main class for reading CSV data.
///
/// The CSV records are read iteratively, regardless of whether the Iterable, the Iterator, or the Stream is used.
/// Once all records are read, the data is consumed. To read the records more than once, collect them in a List or
/// another collection.
///
/// This class is not thread-safe and must not be shared between threads.
///
/// **Exception handling:**
/// - [IOException] is thrown by file/path-based build methods if the file cannot be opened.
/// - [java.io.UncheckedIOException] wraps I/O errors that occur during iteration or streaming.
/// - [CsvParseException] signals structural CSV errors (e.g., malformed quoting, field count mismatch,
///   or exceeded size limits) and wraps any other exception thrown while reading, for example by a [FieldModifier].
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
/// @param <T> the type of the CSV record
public final class CsvReader<T> implements Iterable<T>, Closeable {

    private final CsvParser csvParser;
    private final CsvCallbackHandler<T> callbackHandler;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyLines;
    private final FieldMismatchStrategy extraFieldStrategy;
    private final FieldMismatchStrategy missingFieldStrategy;
    private final boolean fieldCountConsistencyCheck;
    private final CloseableIterator<T> csvRecordIterator = new CsvRecordIterator();

    private int firstRecordFieldCount = -1;
    private boolean parsingStarted;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvReader(final CsvParser csvParser, final CsvCallbackHandler<T> callbackHandler,
              final CommentStrategy commentStrategy, final boolean skipEmptyLines,
              final FieldMismatchStrategy extraFieldStrategy,
              final FieldMismatchStrategy missingFieldStrategy) {

        this.csvParser = csvParser;
        this.callbackHandler = callbackHandler;
        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.extraFieldStrategy = extraFieldStrategy;
        this.missingFieldStrategy = missingFieldStrategy;
        fieldCountConsistencyCheck =
            extraFieldStrategy != FieldMismatchStrategy.IGNORE
            || missingFieldStrategy != FieldMismatchStrategy.IGNORE;
    }

    /// Constructs a [CsvReaderBuilder] to configure and build instances of this class.
    ///
    /// @return a new [CsvReaderBuilder] instance
    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    /// Skips the specified number of lines.
    ///
    /// **Note:** "lines" here means lines ended by `CR`, `LF`, or `CRLF` – not CSV records. Quoting is not taken
    /// into account, so a line break in a quoted field also ends a line. A non-empty last line without a line
    /// break also counts as one line. This method is intended for skipping non-CSV lines that appear before the
    /// actual CSV data starts and must therefore be called before any records are read; once iteration has begun,
    /// this method throws an [IllegalStateException].
    ///
    /// The setting [CsvReaderBuilder#skipEmptyLines(boolean)] has no effect on this method.
    ///
    /// @param lineCount the number of lines to skip
    /// @throws IllegalArgumentException if `lineCount` is negative
    /// @throws IllegalStateException    if any CSV records have already been read
    /// @throws UncheckedIOException     if an I/O error occurs
    /// @throws CsvParseException        if fewer than `lineCount` lines are available or a line does not fit in
    ///                                  the buffer (see [CsvReaderBuilder#maxBufferSize(int)]); with a field
    ///                                  separator of multiple characters or with
    ///                                  [CsvReaderBuilder#trimWhitespacesAroundQuotes(boolean)], a line is
    ///                                  discarded without being buffered and its length is not limited
    public void skipLines(final int lineCount) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount must be non-negative");
        }
        if (parsingStarted) {
            throw new IllegalStateException("skipLines must be called before any CSV records are read");
        }

        int i = 0;
        try {
            for (; i < lineCount; i++) {
                csvParser.skipLine(0);
            }
        } catch (final EOFException e) {
            throw new CsvParseException("Not enough lines to skip. Skipped only %d line(s).".formatted(i), e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Skips lines until the specified predicate matches.
    /// The line that matches the predicate is not skipped.
    ///
    /// The method returns the number of lines actually skipped.
    ///
    /// **Note:** "lines" here means lines ended by `CR`, `LF`, or `CRLF` – not CSV records. A non-empty last line
    /// without a line break also counts as one line. The predicate sees each line as a plain string without its
    /// line break; quoting is not taken into account. This method is intended for skipping non-CSV lines that
    /// appear before the actual CSV data starts and must therefore be called before any records are read; once
    /// iteration has begun, this method throws an [IllegalStateException].
    ///
    /// The setting [CsvReaderBuilder#skipEmptyLines(boolean)] has no effect on this method.
    ///
    /// @param predicate the predicate to match the lines
    /// @param maxLines  the maximum number of lines to test with the predicate, so at most `maxLines - 1` lines are
    ///                  skipped before a match; with `0`, nothing is read and the method returns `0`
    /// @return the number of lines actually skipped
    /// @throws NullPointerException if `predicate` is `null`
    /// @throws IllegalArgumentException if `maxLines` is negative
    /// @throws IllegalStateException if any CSV records have already been read
    /// @throws UncheckedIOException if an I/O error occurs
    /// @throws CsvParseException if none of the first `maxLines` lines matches, the CSV data ends before a match or
    ///     a line does not fit in the buffer (see [CsvReaderBuilder#maxBufferSize(int)])
    public int skipLines(final Predicate<String> predicate, final int maxLines) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        if (maxLines < 0) {
            throw new IllegalArgumentException("maxLines must be non-negative");
        }
        if (parsingStarted) {
            throw new IllegalStateException("skipLines must be called before any CSV records are read");
        }

        if (maxLines == 0) {
            return 0;
        }

        int i = 0;
        try {
            for (; i < maxLines; i++) {
                final String line = csvParser.peekLine();
                if (predicate.test(line)) {
                    return i;
                }
                csvParser.skipLine(line.length());
            }
        } catch (final EOFException e) {
            throw new CsvParseException(
                "No matching line found. Skipped %d line(s) before reaching end of data.".formatted(i), e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        throw new CsvParseException(
            "No matching line found within the maximum limit of %d lines.".formatted(maxLines));
    }

    /// {@return an iterator over the records}
    ///
    /// The returned iterator is not thread-safe.
    /// Remember to close the returned iterator when you're done.
    /// Alternatively, use [#stream()].
    ///
    /// This method is idempotent and can be called multiple times.
    ///
    /// It reads nothing itself; the iterator throws an [UncheckedIOException] if an I/O error occurs and a
    /// [CsvParseException] if parsing fails.
    ///
    /// @see #stream()
    @Override
    public CloseableIterator<T> iterator() {
        return csvRecordIterator;
    }

    /// Constructs a [Spliterator] for traversing the records of this reader; [Spliterator#trySplit()] returns `null`.
    ///
    /// The returned spliterator is not thread-safe.
    /// Remember to invoke [#close()] when you're done.
    /// Alternatively, use [#stream()].
    ///
    /// This method can be called multiple times; all spliterators share the position of this reader.
    ///
    /// It reads nothing itself; the spliterator throws an [UncheckedIOException] if an I/O error occurs and a
    /// [CsvParseException] if parsing fails.
    ///
    /// @return a spliterator over the CSV records
    /// @see #stream()
    @Override
    public Spliterator<T> spliterator() {
        return new CsvSpliterator();
    }

    /// Constructs a new sequential `Stream` with this reader as its source.
    ///
    /// The returned stream is not thread-safe.
    /// Remember to close the returned stream when you're done.
    /// Closing the stream also closes this reader.
    ///
    /// This method can be called multiple times, although it creates a new stream each time.
    ///
    /// The stream is not reusable after it has been closed.
    /// This method reads nothing itself; the stream throws an [UncheckedIOException] if an I/O error occurs and a
    /// [CsvParseException] if parsing fails.
    ///
    /// @return a sequential `Stream` over the CSV records
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

    @Nullable
    private T fetchRecord() throws IOException {
        parsingStarted = true;
        while (csvParser.parse()) {
            final T csvRecord = processRecord();

            if (csvRecord != null) {
                return csvRecord;
            }
        }

        callbackHandler.terminate();

        return null;
    }

    @Nullable
    @SuppressWarnings("checkstyle:ReturnCount")
    private T processRecord() {
        final T csvRecord = callbackHandler.buildRecord();

        // handle consumed records (e.g., header for named records)
        if (csvRecord == null) {
            return null;
        }

        return switch (callbackHandler.getRecordType()) {
            case DATA -> {
                if (fieldCountConsistencyCheck
                    && !checkFieldCountConsistency(callbackHandler.getFieldCount())) {
                    yield null;
                }

                yield csvRecord;
            }
            case COMMENT -> commentStrategy == CommentStrategy.SKIP ? null : csvRecord;
            case EMPTY -> skipEmptyLines ? null : csvRecord;
        };
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    private boolean checkFieldCountConsistency(final int fieldCount) {
        if (firstRecordFieldCount == -1) {
            firstRecordFieldCount = fieldCount;
            return true;
        }

        if (fieldCount > firstRecordFieldCount) {
            return handleMismatch(extraFieldStrategy, fieldCount);
        }

        if (fieldCount < firstRecordFieldCount) {
            return handleMismatch(missingFieldStrategy, fieldCount);
        }

        return true;
    }

    private boolean handleMismatch(final FieldMismatchStrategy strategy, final int fieldCount) {
        return switch (strategy) {
            case STRICT -> throw new CsvParseException(
                "Record %d has %d fields, but first record had %d fields"
                    .formatted(csvParser.getStartingLineNumber(), fieldCount, firstRecordFieldCount));
            case IGNORE -> true;
            case SKIP -> false;
        };
    }

    /// Closes this reader and its data source, including an [InputStream] or [Reader] passed to the builder.
    ///
    /// @throws IOException if an I/O error occurs
    @Override
    public void close() throws IOException {
        csvParser.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvReader.class.getSimpleName() + "[", "]")
            .add("commentStrategy=" + commentStrategy)
            .add("skipEmptyLines=" + skipEmptyLines)
            .add("extraFieldStrategy=" + extraFieldStrategy)
            .add("missingFieldStrategy=" + missingFieldStrategy)
            .add("parser=" + csvParser.getClass().getSimpleName())
            .toString();
    }

    @Nullable
    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
    private T fetch() {
        try {
            return fetchRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(buildExceptionMessage(), e);
        } catch (final CsvParseException e) {
            throw e;
        } catch (final Throwable t) {
            throw new CsvParseException(buildExceptionMessage(), t);
        }
    }

    private String buildExceptionMessage() {
        return (csvParser.getStartingLineNumber() == 1)
            ? "Exception when reading first record"
            : "Exception when reading record that started in line %d".formatted(csvParser.getStartingLineNumber());
    }

    private final class CsvSpliterator implements Spliterator<T> {

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            final T t = fetch();
            if (t != null) {
                action.accept(t);
                return true;
            }
            return false;
        }

        @Nullable
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

    private final class CsvRecordIterator implements CloseableIterator<T> {

        @Nullable
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
    /// configuration of this class follows RFC 4180, except that it allows any character in fields, skips empty lines,
    /// reads empty input as no records and tolerates CR and LF line breaks, a quote character inside an unquoted field
    /// and an unclosed quoted field:
    ///
    /// - Field separator: `,` (comma)
    /// - Quote character: `"` (double quote)
    /// - Comment strategy: [CommentStrategy#NONE] (as comments are not part of RFC 4180)
    /// - Comment character: `#` (hash) (in case comment strategy is enabled)
    /// - Skip empty lines: `true`
    /// - Extra field strategy: [FieldMismatchStrategy#STRICT]
    /// - Missing field strategy: [FieldMismatchStrategy#STRICT]
    /// - Allow extra characters after closing quote characters: `false`
    /// - Allow unclosed quoted field: `true`
    /// - Allow whitespace around quote characters: `false`
    /// - Detect BOM: `false`
    /// - Max buffer size: {@value %,2d #DEFAULT_MAX_BUFFER_SIZE} characters
    ///
    /// CR, LF and CRLF are all recognized as line breaks, even mixed within the same CSV data;
    /// this is not configurable.
    ///
    /// The methods that construct a reader check the configuration. They throw an [IllegalArgumentException] if the
    /// field separator, the quote character or the comment character contains a line break character, or if the
    /// quote character and the first character of the field separator are the same. If the comment strategy is not
    /// [CommentStrategy#NONE], the comment character must differ from these two as well. They throw an
    /// [IllegalStateException] if [#allowExtraCharsAfterClosingQuote(boolean)] is enabled together with a field
    /// separator of multiple characters or with [#trimWhitespacesAroundQuotes(boolean)].
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class CsvReaderBuilder {

        private static final int DEFAULT_MAX_BUFFER_SIZE = 16 * 1024 * 1024;

        private String fieldSeparator = ",";
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean skipEmptyLines = true;
        private FieldMismatchStrategy extraFieldStrategy = FieldMismatchStrategy.STRICT;
        private FieldMismatchStrategy missingFieldStrategy = FieldMismatchStrategy.STRICT;
        private boolean allowExtraCharsAfterClosingQuote;
        private boolean allowUnclosedQuote = true;
        private boolean trimWhitespacesAroundQuotes;
        private boolean detectBomHeader;
        private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;

        private CsvReaderBuilder() {
        }

        /// Sets the `fieldSeparator` used when reading CSV data.
        ///
        /// @param fieldSeparator the field separator character (default: `,`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @see #fieldSeparator(String)
        public CsvReaderBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = String.valueOf(fieldSeparator);
            return this;
        }

        /// Sets the `fieldSeparator` used when reading CSV data.
        ///
        /// Unlike [#fieldSeparator(char)], this method allows specifying a string of multiple characters to
        /// separate fields. The entire string is the field separator: fields are only separated where the
        /// **full string** occurs. Its individual characters are **not** field separators on their own.
        ///
        /// **If multiple characters are used, a less performant parser is used.**
        ///
        /// @param fieldSeparator the field separator string (default: `,`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @throws IllegalArgumentException if `fieldSeparator` is `null` or empty
        /// @see #fieldSeparator(char)
        public CsvReaderBuilder fieldSeparator(final String fieldSeparator) {
            if (fieldSeparator == null || fieldSeparator.isEmpty()) {
                throw new IllegalArgumentException("fieldSeparator must not be null or empty");
            }
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /// Sets the `quoteCharacter` used when reading CSV data.
        ///
        /// @param quoteCharacter the quote character (default: `"`)
        /// @return This updated object, allowing additional method calls to be chained together
        public CsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /// Sets the strategy for handling comments
        /// (default: [CommentStrategy#NONE] as comments are not part of RFC 4180).
        ///
        /// With any other strategy, where a record would begin with the comment character, a comment is read
        /// instead. Everything after the comment character up to the next line break is the comment text.
        ///
        /// @param commentStrategy the strategy for handling comments
        /// @return This updated object, allowing additional method calls to be chained together
        /// @see #commentCharacter(char)
        public CsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
            this.commentStrategy = commentStrategy;
            return this;
        }

        /// Sets the `commentCharacter` used when reading CSV data.
        ///
        /// @param commentCharacter the character that begins a comment (default: `#`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @see #commentStrategy(CommentStrategy)
        public CsvReaderBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /// Defines whether empty lines should be skipped when reading data.
        ///
        /// An empty line has no characters before its line break (no whitespace, no quote characters, nothing).
        /// Unless skipped, it is read as a record with one empty field.
        ///
        /// Comments are not affected by this setting; see [#commentStrategy(CommentStrategy)].
        ///
        /// @param skipEmptyLines whether empty lines should be skipped (default: `true`)
        /// @return This updated object, allowing additional method calls to be chained together
        public CsvReaderBuilder skipEmptyLines(final boolean skipEmptyLines) {
            this.skipEmptyLines = skipEmptyLines;
            return this;
        }

        /// Defines the strategy for handling records that contain more fields than the first record.
        /// The comparison uses the first record that the reader returns. Empty lines and comments never count as the
        /// first record, and neither does a header that is not returned (see
        /// [NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#returnHeader(boolean)]).
        ///
        /// @param extraFieldStrategy the strategy for extra fields (default: [FieldMismatchStrategy#STRICT])
        /// @return This updated object, allowing additional method calls to be chained together
        /// @throws NullPointerException if `extraFieldStrategy` is `null`
        /// @see #missingFieldStrategy(FieldMismatchStrategy)
        public CsvReaderBuilder extraFieldStrategy(final FieldMismatchStrategy extraFieldStrategy) {
            this.extraFieldStrategy =
                Objects.requireNonNull(extraFieldStrategy, "extraFieldStrategy must not be null");
            return this;
        }

        /// Defines the strategy for handling records that contain fewer fields than the first record.
        /// The comparison uses the first record that the reader returns. Empty lines and comments never count as the
        /// first record, and neither does a header that is not returned (see
        /// [NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#returnHeader(boolean)]).
        ///
        /// Empty lines are allowed even if this is set to [FieldMismatchStrategy#STRICT].
        ///
        /// @param missingFieldStrategy the strategy for missing fields
        ///                             (default: [FieldMismatchStrategy#STRICT])
        /// @return This updated object, allowing additional method calls to be chained together
        /// @throws NullPointerException if `missingFieldStrategy` is `null`
        /// @see #extraFieldStrategy(FieldMismatchStrategy)
        /// @see #skipEmptyLines(boolean)
        public CsvReaderBuilder missingFieldStrategy(final FieldMismatchStrategy missingFieldStrategy) {
            this.missingFieldStrategy =
                Objects.requireNonNull(missingFieldStrategy, "missingFieldStrategy must not be null");
            return this;
        }

        /// Defines whether a [CsvParseException] should be thrown if records contain
        /// more fields than the first record.
        /// The comparison uses the first record that the reader returns. Empty lines and comments never count as the
        /// first record, and neither does a header that is not returned (see
        /// [NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#returnHeader(boolean)]).
        ///
        /// @param allowExtraFields whether extra fields should be allowed (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @deprecated Use [#extraFieldStrategy(FieldMismatchStrategy)] instead
        @Deprecated(forRemoval = true)
        public CsvReaderBuilder allowExtraFields(final boolean allowExtraFields) {
            this.extraFieldStrategy = allowExtraFields
                ? FieldMismatchStrategy.IGNORE : FieldMismatchStrategy.STRICT;
            return this;
        }

        /// Defines whether a [CsvParseException] should be thrown if records contain
        /// fewer fields than the first record.
        /// The comparison uses the first record that the reader returns. Empty lines and comments never count as the
        /// first record, and neither does a header that is not returned (see
        /// [NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#returnHeader(boolean)]).
        ///
        /// Empty lines are allowed even if this is set to `false`.
        ///
        /// @param allowMissingFields whether missing fields should be allowed (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @deprecated Use [#missingFieldStrategy(FieldMismatchStrategy)] instead
        @Deprecated(forRemoval = true)
        public CsvReaderBuilder allowMissingFields(final boolean allowMissingFields) {
            this.missingFieldStrategy = allowMissingFields
                ? FieldMismatchStrategy.IGNORE : FieldMismatchStrategy.STRICT;
            return this;
        }

        /// Specifies whether the presence of characters between a closing quote character and the next field
        /// separator, line break or the end of the CSV data should be treated as an error or not.
        ///
        /// Example: `"a"b,"c"`
        ///
        /// If this is set to `true`, the first field is returned as `ab`.
        ///
        /// If this is set to `false`, a [CsvParseException] is thrown.
        ///
        /// This option cannot be combined with a field separator of multiple characters or with
        /// [#trimWhitespacesAroundQuotes(boolean)]; constructing the reader then throws an [IllegalStateException].
        ///
        /// @param allowExtraCharsAfterClosingQuote allow extra characters after closing quote characters
        ///     (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together
        /// @see #trimWhitespacesAroundQuotes(boolean)
        /// @deprecated This option permits non-conforming input (RFC 4180 does not allow any character between
        ///     a closing quote character and the next field separator or line break) and yields unspecified
        ///     results if the extra characters contain quote characters.
        ///     Use [#trimWhitespacesAroundQuotes(boolean)] instead
        @Deprecated(forRemoval = true)
        public CsvReaderBuilder allowExtraCharsAfterClosingQuote(final boolean allowExtraCharsAfterClosingQuote) {
            this.allowExtraCharsAfterClosingQuote = allowExtraCharsAfterClosingQuote;
            return this;
        }

        /// Defines whether the CSV data may end inside a quoted field.
        ///
        /// Example: `"foo,bar`
        ///
        /// If this is set to `true`, the field content `foo,bar` is returned as a single field; otherwise,
        /// a [CsvParseException] is thrown.
        ///
        /// Independent of this flag, the limit of [#maxBufferSize(int)] applies to an unclosed quoted field.
        ///
        /// **The default will change to `false` in version 5.0.**
        ///
        /// @param allowUnclosedQuote tolerate an unclosed quoted field (default: `true`)
        /// @return This updated object, allowing additional method calls to be chained together
        public CsvReaderBuilder allowUnclosedQuote(final boolean allowUnclosedQuote) {
            this.allowUnclosedQuote = allowUnclosedQuote;
            return this;
        }

        /// Defines whether whitespace before an opening quote character and after a closing quote character
        /// is allowed and removed.
        ///
        /// This is a tolerance of malformed CSV data: RFC 4180 does not allow any character between a quote
        /// character and the adjacent field separator or line break.
        /// Without this option, such whitespace has these effects:
        ///
        /// - Whitespace before an opening quote character makes the field an unquoted field. The input ` "foo,bar"`
        ///   is then read as two fields with the field contents ` "foo` and `bar"`.
        /// - Whitespace after a closing quote character causes a [CsvParseException], unless
        ///   [#allowExtraCharsAfterClosingQuote(boolean)] is enabled. In that case, the whitespace becomes part of
        ///   the field content.
        ///
        /// Enabling this option tolerates such whitespace
        /// (*whitespace is shown as underscores (`_`) for clarity*):
        ///
        /// | Encoded field | Field content |
        /// |---------------|---------------|
        /// | `_"x"_`       | `x`           |
        /// | `_"foo,bar"_` | `foo,bar`     |
        /// | `"_x_"`       | `_x_`         |
        /// | `_x_`         | `_x_`         |
        ///
        /// Whitespace in this context is any character from `U+0000` to `U+0020` (the space character) –
        /// the same logic as in Java's [String#trim()] method.
        ///
        /// **When enabling this, a less performant parser is used.**
        ///
        /// @param trimWhitespacesAroundQuotes if whitespace should be allowed and removed (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together
        public CsvReaderBuilder trimWhitespacesAroundQuotes(final boolean trimWhitespacesAroundQuotes) {
            this.trimWhitespacesAroundQuotes = trimWhitespacesAroundQuotes;
            return this;
        }

        /// Defines whether a byte order mark (BOM) at the beginning of the data should be detected.
        ///
        /// A detected BOM determines the character set and is removed. Without detection, a BOM is decoded with
        /// the configured character set; with UTF-8, it remains as `U+FEFF` at the start of the first field's
        /// content.
        ///
        /// **BOM detection only applies to [InputStream] and [Path] based data sources.
        /// It does not apply to [Reader] or [String] based data sources, as they are already decoded.**
        ///
        /// Supported BOMs are: UTF-8, UTF-16LE, UTF-16BE, UTF-32LE, UTF-32BE.
        ///
        /// @param detectBomHeader if detection should be enabled (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together
        public CsvReaderBuilder detectBomHeader(final boolean detectBomHeader) {
            this.detectBomHeader = detectBomHeader;
            return this;
        }

        /// Defines the maximum buffer size used when parsing data.
        ///
        /// The size of the internal buffer is automatically adjusted to the needs of the parser.
        /// To protect against out-of-memory errors, its maximum size is limited; if the buffer would have to grow
        /// beyond it, a [CsvParseException] is thrown.
        ///
        /// The buffer is used for several purposes:
        ///   - Reading data from the underlying stream of data in chunks
        ///   - Storing the data of a single field before it is passed to the callback handler
        ///   - Buffering a full line when skipping lines with [CsvReader#skipLines(Predicate, int)] and, unless the
        ///     field separator has multiple characters or [#trimWhitespacesAroundQuotes(boolean)] is enabled, with
        ///     [CsvReader#skipLines(int)]
        ///
        /// The parser needs free space for reading up to 8,192 characters at once. Depending on how many characters
        /// the data source returns per read, a field can therefore exceed the limit although it is up to
        /// 8,192 characters smaller than this value.
        ///
        /// When reading from a [String], the data is already in memory and this limit does not apply. The exception
        /// is a field separator of multiple characters or [#trimWhitespacesAroundQuotes(boolean)]: with these, the
        /// limit applies to each field.
        ///
        /// Set a larger value only if you expect to read fields or lines larger than the default limit.
        /// In that case you probably **also need to adjust** the maximum field size of the callback handler.
        ///
        /// Set a smaller value if your runtime environment does not have enough memory for the default value.
        /// Setting values smaller than 16,384 characters most likely degrades performance.
        ///
        /// @param maxBufferSize the maximum buffer size in characters (default: {@value %,2d #DEFAULT_MAX_BUFFER_SIZE})
        /// @return This updated object, allowing additional method calls to be chained together
        /// @throws IllegalArgumentException if `maxBufferSize` is not positive
        public CsvReaderBuilder maxBufferSize(final int maxBufferSize) {
            Preconditions.checkArgument(maxBufferSize > 0, "maxBufferSize must be greater than 0");
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /// Convenience method to read a single CSV record from the specified string.
        ///
        /// If the string contains multiple records, only the first one is read and returned.
        ///
        /// @param data the CSV data to read; must not be `null`
        /// @return a single [CsvRecord] instance containing the parsed data, never `null`
        /// @throws NullPointerException     if `data` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @throws CsvParseException        if the data contains no record or cannot be parsed
        /// @see #ofSingleCsvRecord(CsvCallbackHandler, String)
        public CsvRecord ofSingleCsvRecord(final String data) {
            return ofSingleCsvRecord(CsvRecordHandler.of(), data);
        }

        /// Convenience method to read a single CSV record using a custom callback handler.
        ///
        /// If the string contains multiple records, only the first one is read and returned.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param data            the CSV data to read; must not be `null`
        /// @return a single record as processed by the callback handler, never `null`
        /// @throws NullPointerException     if `callbackHandler` or `data` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @throws CsvParseException        if the data contains no record that the callback handler returns (a header
        ///     read by [NamedCsvRecordHandler] is not returned) or cannot be parsed
        /// @see #ofSingleCsvRecord(String)
        public <T> T ofSingleCsvRecord(final CsvCallbackHandler<T> callbackHandler, final String data) {
            final T fetchedRecord = build(callbackHandler, data).fetch();
            if (fetchedRecord == null) {
                throw new CsvParseException("No record found in the provided data");
            }
            return fetchedRecord;
        }

        /// Constructs a new index-based [CsvReader] for the specified input stream.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// @param inputStream the input stream to read data from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `inputStream` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofCsvRecord(InputStream, Charset)
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream) {
            return build(CsvRecordHandler.of(), inputStream);
        }

        /// Constructs a new index-based [CsvReader] for the specified input stream and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream,Charset)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// @param inputStream the input stream to read data from
        /// @param charset     the character set to use. If BOM detection is enabled
        ///                    (via [#detectBomHeader(boolean)]), this acts as a default
        ///                    when no BOM is found
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `inputStream` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofCsvRecord(InputStream)
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream, final Charset charset) {
            return build(CsvRecordHandler.of(), inputStream, charset);
        }

        /// Constructs a new index-based [CsvReader] for the specified reader.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Reader)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param reader the data source to read from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `reader` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public CsvReader<CsvRecord> ofCsvRecord(final Reader reader) {
            return build(CsvRecordHandler.of(), reader);
        }

        /// Constructs a new index-based [CsvReader] for the specified String.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,String)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param data the data to read
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `data` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public CsvReader<CsvRecord> ofCsvRecord(final String data) {
            return build(CsvRecordHandler.of(), data);
        }

        /// Constructs a new index-based [CsvReader] for the specified file.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// @param file the file to read data from
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `file` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofCsvRecord(Path, Charset)
        public CsvReader<CsvRecord> ofCsvRecord(final Path file) throws IOException {
            return build(CsvRecordHandler.of(), file);
        }

        /// Constructs a new index-based [CsvReader] for the specified file and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path,Charset)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// @param file    the file to read data from
        /// @param charset the character set to use. If BOM detection is enabled
        ///                (via [#detectBomHeader(boolean)]), this acts as a default
        ///                when no BOM is found
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `file` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofCsvRecord(Path)
        public CsvReader<CsvRecord> ofCsvRecord(final Path file, final Charset charset) throws IOException {
            return build(CsvRecordHandler.of(), file, charset);
        }

        /// Constructs a new name-based [CsvReader] for the specified input stream.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// @param inputStream the input stream to read data from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `inputStream` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofNamedCsvRecord(InputStream, Charset)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream) {
            return build(NamedCsvRecordHandler.of(), inputStream);
        }

        /// Constructs a new name-based [CsvReader] for the specified input stream and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,InputStream,Charset)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// @param inputStream the input stream to read data from
        /// @param charset     the character set to use. If BOM detection is enabled
        ///                    (via [#detectBomHeader(boolean)]), this acts as a default
        ///                    when no BOM is found
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `inputStream` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofNamedCsvRecord(InputStream)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream, final Charset charset) {
            return build(NamedCsvRecordHandler.of(), inputStream, charset);
        }

        /// Constructs a new name-based [CsvReader] for the specified reader.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Reader)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param reader the data source to read from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `reader` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Reader reader) {
            return build(NamedCsvRecordHandler.of(), reader);
        }

        /// Constructs a new name-based [CsvReader] for the specified String.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,String)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param data the data to read
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `data` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final String data) {
            return build(NamedCsvRecordHandler.of(), data);
        }

        /// Constructs a new name-based [CsvReader] for the specified file.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// @param file the file to read data from
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `file` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @see #ofNamedCsvRecord(Path, Charset)
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Path file) throws IOException {
            return build(NamedCsvRecordHandler.of(), file);
        }

        /// Constructs a new name-based [CsvReader] for the specified file and character set.
        ///
        /// This is a convenience method for calling [#build(CsvCallbackHandler,Path,Charset)] with
        /// [NamedCsvRecordHandler] as the callback handler.
        ///
        /// @param file    the file to read data from
        /// @param charset the character set to use. If BOM detection is enabled
        ///                (via [#detectBomHeader(boolean)]), this acts as a default
        ///                when no BOM is found
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `file` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
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
        /// implementation such as [java.io.BufferedInputStream]. Performance may even be better if you do not.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// Use [#build(CsvCallbackHandler,Path)] for optimal performance when reading files.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param inputStream     the input stream to read data from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `callbackHandler` or `inputStream` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        /// @see #build(CsvCallbackHandler, InputStream, Charset)
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final InputStream inputStream) {
            return build(callbackHandler, inputStream, StandardCharsets.UTF_8);
        }

        /// Constructs a new callback-based [CsvReader] for the specified input stream and character set.
        ///
        /// This library uses built-in buffering, so you do not need to pass in a buffered InputStream
        /// implementation such as [java.io.BufferedInputStream]. Performance may even be better if you do not.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the BOM is read to determine the character
        /// set when reading starts – this method itself does not consume the input stream.
        ///
        /// Use [#build(CsvCallbackHandler,Path,Charset)] for optimal performance when reading files.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param inputStream     the input stream to read data from
        /// @param charset         the character set to use. If BOM detection is enabled
        ///                        (via [#detectBomHeader(boolean)]), this acts as a default
        ///                        when no BOM is found
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `callbackHandler`, `inputStream` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
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
        /// implementation such as [java.io.BufferedReader]. Performance may even be better if you do not.
        ///
        /// Use [#build(CsvCallbackHandler,Path)] for optimal performance when
        /// reading files and [#build(CsvCallbackHandler,String)] when reading Strings.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param reader          the data source to read from
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `callbackHandler` or `reader` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Reader reader) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(reader, "reader must not be null");

            final CsvParser csvParser;
            if (isRelaxedConfiguration()) {
                csvParser = new RelaxedCsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                    commentCharacter, trimWhitespacesAroundQuotes, allowUnclosedQuote, callbackHandler,
                    maxBufferSize, reader
                );
            } else {
                csvParser = new StrictCsvParser(fieldSeparator.charAt(0), quoteCharacter, commentStrategy,
                    commentCharacter, allowExtraCharsAfterClosingQuote, allowUnclosedQuote,
                    callbackHandler, maxBufferSize, reader);
            }

            return newReader(callbackHandler, csvParser);
        }

        /// Constructs a new callback-based [CsvReader] for the specified String.
        ///
        /// [#detectBomHeader(boolean)] has no effect on this method.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param data            the data to read
        /// @return a new CsvReader, never `null`
        /// @throws NullPointerException if `callbackHandler` or `data` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException if the configured options conflict (see [CsvReaderBuilder])
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final String data) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(data, "data must not be null");

            final CsvParser csvParser;
            if (isRelaxedConfiguration()) {
                csvParser = new RelaxedCsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                    commentCharacter, trimWhitespacesAroundQuotes, allowUnclosedQuote, callbackHandler,
                    maxBufferSize, data
                );
            } else {
                csvParser = new StrictCsvParser(fieldSeparator.charAt(0), quoteCharacter, commentStrategy,
                    commentCharacter, allowExtraCharsAfterClosingQuote, allowUnclosedQuote,
                    callbackHandler, data);
            }

            return newReader(callbackHandler, csvParser);
        }

        /// Constructs a new callback-based [CsvReader] for the specified file.
        ///
        /// If [#detectBomHeader(boolean)] is enabled, the character set is determined by the BOM.
        /// By default, the character set is [StandardCharsets#UTF_8].
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param file            the file to read data from
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `callbackHandler` or `file` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @see #build(CsvCallbackHandler, Path, Charset)
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Path file)
            throws IOException {
            return build(callbackHandler, file, StandardCharsets.UTF_8);
        }

        /// Constructs a new callback-based [CsvReader] for the specified file and character set.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use; do not reuse a handler after it has been used
        /// @param file            the file to read data from
        /// @param charset         the character set to use. If BOM detection is enabled
        ///                        (via [#detectBomHeader(boolean)]), this acts as a default
        ///                        when no BOM is found
        /// @return a new CsvReader, never `null`; remember to close it
        /// @throws IOException              if an I/O error occurs
        /// @throws NullPointerException     if `callbackHandler`, `file` or `charset` is `null`
        /// @throws IllegalArgumentException if the configured characters are not valid (see [CsvReaderBuilder])
        /// @throws IllegalStateException    if the configured options conflict (see [CsvReaderBuilder])
        /// @see #build(CsvCallbackHandler, Path)
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                      final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            // No configuration pre-check here, deliberately: unlike CsvWriter, opening for read has no
            // side effect to undo, so closing the stream in buildOwning is already a complete fix.
            return buildOwning(callbackHandler, Files.newInputStream(file), charset);
        }

        /// Like [#build(CsvCallbackHandler, InputStream, Charset)] but takes ownership of the stream:
        /// if reader construction fails, the stream is closed before the exception propagates. The
        /// caller never receives a handle it could close itself.
        ///
        /// Only for streams this library opened itself – streams passed in by the caller stay owned by
        /// the caller and must not be closed here.
        ///
        /// @param <T>             the type of the CSV record
        /// @param callbackHandler the callback handler to use
        /// @param inputStream     the library-opened stream to read data from
        /// @param charset         the character set to use
        /// @return a new CsvReader, never `null`
        @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
        <T> CsvReader<T> buildOwning(final CsvCallbackHandler<T> callbackHandler,
                                     final InputStream inputStream, final Charset charset) {
            try {
                return build(callbackHandler, inputStream, charset);
            } catch (final RuntimeException | Error e) {
                // Catching broadly is the point: whatever construction throws, the stream must not survive it.
                try {
                    inputStream.close();
                } catch (final Exception | Error closeException) {
                    // Mirrors try-with-resources: a failure to close never displaces the failure that
                    // caused the close, whether it is checked or not.
                    e.addSuppressed(closeException);
                }
                throw e;
            }
        }

        private boolean isRelaxedConfiguration() {
            final boolean relaxed = isForceRelaxedParser()
                || fieldSeparator.length() > 1 || trimWhitespacesAroundQuotes;
            if (relaxed && allowExtraCharsAfterClosingQuote) {
                throw new IllegalStateException("allowExtraCharsAfterClosingQuote is not supported in relaxed mode");
            }
            return relaxed;
        }

        // Forcing the use of the relaxed parser is only necessary for testing purposes.
        private static boolean isForceRelaxedParser() {
            return Boolean.parseBoolean(System.getProperty("de.siegmar.fastcsv.relaxed", "false"));
        }

        private <T> CsvReader<T> newReader(final CsvCallbackHandler<T> callbackHandler, final CsvParser csvParser) {
            return new CsvReader<>(csvParser, callbackHandler,
                commentStrategy, skipEmptyLines, extraFieldStrategy, missingFieldStrategy);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CsvReaderBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentStrategy=" + commentStrategy)
                .add("commentCharacter=" + commentCharacter)
                .add("skipEmptyLines=" + skipEmptyLines)
                .add("extraFieldStrategy=" + extraFieldStrategy)
                .add("missingFieldStrategy=" + missingFieldStrategy)
                .add("allowExtraCharsAfterClosingQuote=" + allowExtraCharsAfterClosingQuote)
                .add("allowUnclosedQuote=" + allowUnclosedQuote)
                .add("trimWhitespacesAroundQuotes=" + trimWhitespacesAroundQuotes)
                .add("detectBomHeader=" + detectBomHeader)
                .add("maxBufferSize=" + maxBufferSize)
                .toString();
        }

    }

}
