package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.siegmar.fastcsv.util.Nullable;
import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/// CSV reader implementation for indexed based access.
///
/// If no prebuilt index passed in (via [IndexedCsvReaderBuilder#index(CsvIndex)]) the constructor will initiate
/// indexing the file.
/// This process is optimized on performance and low memory usage – no CSV data is stored in memory.
/// The current status can be monitored via [IndexedCsvReaderBuilder#statusListener(StatusListener)].
///
/// The index itself is held in memory, though: roughly 40 bytes per [CsvIndex.CsvPage], of which there is one
/// per [IndexedCsvReaderBuilder#pageSize(int)] records. Index memory therefore scales with the record count,
/// not with the file size.
///
/// As indexing is performed on a byte level, only **UTF-8 and single-byte charsets** (such as ISO-8859-1 or
/// US-ASCII) are supported; anything else is rejected with an [IllegalArgumentException]. This also applies to
/// a charset detected via a BOM.
///
/// Other multibyte charsets cannot be indexed byte-wise, for either of two reasons. UTF-16, UTF-32 and the
/// variable-width legacy charsets (Shift_JIS, Big5, GBK, GB18030, ISO-2022-*, …) put ASCII byte values
/// *inside* the encoding of other characters, where the indexer mistakes them for structure. Others are
/// ASCII-transparent when encoding but consume a following byte when *decoding* — EUC-JP decodes any byte
/// pair starting `0xA1`–`0xFE` as one character, swallowing a line feed or separator the indexer already
/// counted. Single-byte charsets must additionally map every ASCII byte to its own character in both
/// directions, which rules out EBCDIC and charsets that map an ASCII byte elsewhere.
///
/// For any of these, transcode the file to UTF-8 or read it with [CsvReader], which is character-oriented
/// and supports every charset.
///
/// This class is thread-safe.
///
/// Example use:
/// ```
/// try (IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder().ofCsvRecord(file)) {
///     CsvIndex index = csv.getIndex();
///     int lastPage = index.pages().size() - 1;
///     List<CsvRecord> csvRecords = csv.readPage(lastPage);
/// }
/// ```
///
/// @param <T> the type of the CSV record.
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public final class IndexedCsvReader<T> implements Closeable {

    /// Highest value a control character may have; the byte-oriented indexer cannot handle any other.
    private static final int MAX_BASE_ASCII = 127;

    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final boolean allowExtraCharsAfterClosingQuote;
    private final boolean allowUnclosedQuote;
    private final int pageSize;
    private final SeekableInputStreamReader reader;
    private final Lock fileLock = new ReentrantLock();
    private final CsvCallbackHandler<T> csvRecordHandler;
    private final CsvParser csvParser;
    private final CsvIndex csvIndex;

    @SuppressWarnings("checkstyle:ParameterNumber")
    IndexedCsvReader(final Path file, final Charset defaultCharset,
                     final char fieldSeparator, final char quoteCharacter,
                     final CommentStrategy commentStrategy, final char commentCharacter,
                     final boolean allowExtraCharsAfterClosingQuote,
                     final boolean allowUnclosedQuote,
                     final int maxBufferSize,
                     final int pageSize,
                     final CsvCallbackHandler<T> csvRecordHandler,
                     @Nullable final CsvIndex csvIndex,
                     final StatusListener statusListener)
        throws IOException {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter, commentStrategy);

        this.file = file;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.allowExtraCharsAfterClosingQuote = allowExtraCharsAfterClosingQuote;
        this.allowUnclosedQuote = allowUnclosedQuote;
        this.pageSize = pageSize;
        this.csvRecordHandler = csvRecordHandler;

        // Detect potential BOM and use the detected charset
        final Optional<BomHeader> optionalBomHeader = detectBom(file, statusListener);
        final int bomHeaderLength;
        if (optionalBomHeader.isEmpty()) {
            charset = defaultCharset;
            bomHeaderLength = 0;
        } else {
            final var bomHeader = optionalBomHeader.get();
            charset = bomHeader.getCharset();
            bomHeaderLength = bomHeader.getLength();
        }

        // Applies to the BOM-detected charset just as much as to the supplied one.
        Preconditions.checkArgument(mapsAsciiToIdenticalBytes(charset), () -> """
            Charset '%s' is not supported by IndexedCsvReader, which builds its index on byte level. \
            Only UTF-8 and single-byte charsets that map ASCII to identical single bytes are supported. \
            Transcode the file to UTF-8 or use CsvReader, which reads sequentially and supports any \
            charset.\
            """.formatted(charset.name()));

        if (csvIndex != null) {
            this.csvIndex = validatePrebuiltIndex(file, bomHeaderLength,
                (byte) fieldSeparator, (byte) quoteCharacter, commentStrategy, (byte) commentCharacter,
                pageSize, csvIndex);
        } else {
            this.csvIndex = buildIndex(bomHeaderLength, statusListener);
        }

        reader = new SeekableInputStreamReader(new RandomAccessFile(file.toFile(), "r"), charset);
        csvParser = new StrictCsvParser(fieldSeparator, quoteCharacter, commentStrategy, commentCharacter,
            allowExtraCharsAfterClosingQuote, allowUnclosedQuote, csvRecordHandler, maxBufferSize, reader);

        if (csvRecordHandler instanceof NamedCsvRecordHandler && !this.csvIndex.pages().isEmpty()) {
            captureHeader();
        }
    }

    private static void assertFields(final char fieldSeparator, final char quoteCharacter,
                                     final char commentCharacter, final CommentStrategy commentStrategy) {
        if (commentStrategy == CommentStrategy.NONE) {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator, quoteCharacter), () ->
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s)".formatted(
                    fieldSeparator, quoteCharacter));
        } else {
            Preconditions.checkArgument(!Util.containsDupe(fieldSeparator, quoteCharacter, commentCharacter), () ->
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)".formatted(
                    fieldSeparator, quoteCharacter, commentCharacter));
        }
    }

    /*
     * CsvScanner builds the index from raw bytes while the pages are decoded and parsed as characters.
     * That only holds together if every ASCII byte is exactly its own character: control characters are
     * restricted to ASCII (see MAX_BASE_ASCII), so verifying the whole range covers all of them at once,
     * in both directions. It rules out EBCDIC (comma at 0x6B) and charsets that map an ASCII byte to a
     * different character (IBM864 decodes 0x25 to U+066A).
     *
     * Multibyte charsets are excluded outright: the bytes of their multibyte characters fall inside the
     * ASCII range (Shift_JIS, GBK and Big5 use 0x40-0x7E, GB18030 also 0x30-0x39, and the stateful
     * ISO-2022-* family uses 0x21-0x7E, covering even the default control characters), so the scanner
     * would read data as structure. UTF-8 is the one safe multibyte charset - its sequences use lead
     * bytes >= 0xC2 and continuation bytes 0x80-0xBF, never an ASCII byte.
     */
    private static boolean mapsAsciiToIdenticalBytes(final Charset charset) {
        if (StandardCharsets.UTF_8.equals(charset)) {
            // The default charset, and the only multibyte one that qualifies - no need to probe it.
            return true;
        }
        if (!charset.canEncode() || charset.newEncoder().maxBytesPerChar() > 1) {
            return false;
        }

        final byte[] asciiBytes = new byte[MAX_BASE_ASCII + 1];
        for (int i = 0; i < asciiBytes.length; i++) {
            asciiBytes[i] = (byte) i;
        }
        final String asciiChars = new String(asciiBytes, StandardCharsets.US_ASCII);

        return Arrays.equals(asciiChars.getBytes(charset), asciiBytes)
            && asciiChars.equals(new String(asciiBytes, charset));
    }

    private static Optional<BomHeader> detectBom(final Path file, final StatusListener statusListener)
        throws IOException {
        try {
            return BomUtil.detectCharset(file);
        } catch (final IOException e) {
            statusListener.onError(e);
            throw e;
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static CsvIndex validatePrebuiltIndex(final Path file, final int bomHeaderLength, final byte fieldSeparator,
                                                  final byte quoteCharacter, final CommentStrategy commentStrategy,
                                                  final byte commentCharacter, final int pageSize,
                                                  final CsvIndex csvIndex)
        throws IOException {
        final var expectedSignature = new StringJoiner(", ")
            .add("bomHeaderLength=" + bomHeaderLength)
            .add("fileSize=" + Files.size(file))
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .add("pageSize=" + pageSize)
            .toString();
        final var actualSignature = new StringJoiner(", ")
            .add("bomHeaderLength=" + csvIndex.bomHeaderLength())
            .add("fileSize=" + csvIndex.fileSize())
            .add("fieldSeparator=" + csvIndex.fieldSeparator())
            .add("quoteCharacter=" + csvIndex.quoteCharacter())
            .add("commentStrategy=" + csvIndex.commentStrategy())
            .add("commentCharacter=" + csvIndex.commentCharacter())
            .add("pageSize=" + csvIndex.pageSize())
            .toString();

        Preconditions.checkArgument(expectedSignature.equals(actualSignature), () ->
            "Index does not match! Expected: %s; Actual: %s".formatted(
            expectedSignature, actualSignature));

        return csvIndex;
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
    private CsvIndex buildIndex(final int bomHeaderLength, final StatusListener statusListener) throws IOException {
        final var listener = new ScannerListener(statusListener);

        try (var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            statusListener.onInit(channel.size());

            new CsvScanner(channel,
                bomHeaderLength,
                (byte) fieldSeparator,
                (byte) quoteCharacter,
                commentStrategy,
                (byte) commentCharacter,
                listener
            ).scan();

            final var idx = new CsvIndex(bomHeaderLength, channel.size(), (byte) fieldSeparator, (byte) quoteCharacter,
                commentStrategy, (byte) commentCharacter, pageSize,
                listener.recordCounter.get(), listener.pageOffsets);

            statusListener.onComplete();
            return idx;
        } catch (final Throwable t) {
            statusListener.onError(t);
            throw t;
        }
    }

    /// Constructs a [IndexedCsvReaderBuilder] to configure and build instances of
    /// this class.
    ///
    /// @return a new [IndexedCsvReaderBuilder] instance.
    public static IndexedCsvReaderBuilder builder() {
        return new IndexedCsvReaderBuilder();
    }

    /// Get the index used for accessing the CSV file.
    /// That index is either a freshly built index or the index that has been
    /// passed via [IndexedCsvReaderBuilder#index(CsvIndex)].
    ///
    /// @return the index that is used for accessing the CSV file.
    public CsvIndex getIndex() {
        return csvIndex;
    }

    /// Reads a page of records.
    ///
    /// @param page the page to read (0-based).
    /// @return a page of records, never `null`.
    /// @throws IOException               if an I/O error occurs.
    /// @throws IllegalArgumentException  if `page` is &lt; 0
    /// @throws IndexOutOfBoundsException if the file does not contain the specified page
    public List<T> readPage(final int page) throws IOException {
        Preconditions.checkArgument(page >= 0, "page must be >= 0");
        return readPage(csvIndex.pages().get(page));
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
    private List<T> readPage(final CsvIndex.CsvPage page) throws IOException {
        final List<T> ret = new ArrayList<>(pageSize);
        fileLock.lock();
        try {
            reader.seek(page.offset());
            csvParser.reset(page.startingLineNumber() - 1);

            for (int i = 0; i < pageSize && csvParser.parse(); i++) {
                final T rec = csvRecordHandler.buildRecord();
                if (rec != null) {
                    ret.add(rec);
                }
            }
        } catch (final IOException e) {
            throw new IOException(buildExceptionMessage(), e);
        } catch (final CsvParseException e) {
            throw e;
        } catch (final Throwable t) {
            throw new CsvParseException(buildExceptionMessage(), t);
        } finally {
            fileLock.unlock();
        }
        return ret;
    }

    /// Feeds the first data record to the callback handler, which captures its header from it
    /// (unless predefined), so that page reads are independent of the order in which pages are requested.
    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingGenericException"})
    private void captureHeader() throws IOException {
        try {
            final CsvIndex.CsvPage firstPage = csvIndex.pages().get(0);
            reader.seek(firstPage.offset());
            csvParser.reset(firstPage.startingLineNumber() - 1);

            while (csvParser.parse()) {
                if (csvRecordHandler.getRecordType() == RecordType.DATA) {
                    csvRecordHandler.buildRecord();
                    break;
                }
            }
        } catch (final IOException e) {
            throw closeAfterFailure(new IOException(buildExceptionMessage(), e));
        } catch (final CsvParseException e) {
            throw closeAfterFailure(e);
        } catch (final Throwable t) {
            throw closeAfterFailure(new CsvParseException(buildExceptionMessage(), t));
        }
    }

    private <E extends Throwable> E closeAfterFailure(final E exception) {
        try {
            reader.close();
        } catch (final IOException e) {
            exception.addSuppressed(e);
        }
        return exception;
    }

    private String buildExceptionMessage() {
        return (csvParser.getStartingLineNumber() == 1)
            ? "Exception when reading first record"
            : "Exception when reading record that started in line %d".formatted(csvParser.getStartingLineNumber());
    }

    @Override
    public void close() throws IOException {
        csvParser.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IndexedCsvReader.class.getSimpleName() + "[", "]")
            .add("file=" + file)
            .add("charset=" + charset)
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .add("allowExtraCharsAfterClosingQuote=" + allowExtraCharsAfterClosingQuote)
            .add("allowUnclosedQuote=" + allowUnclosedQuote)
            .add("pageSize=" + pageSize)
            .add("index=" + csvIndex)
            .toString();
    }

    /// This builder is used to create configured instances of [IndexedCsvReader]. The default
    /// configuration of this class adheres with RFC 4180:
    ///
    /// - Field separator: `,` (comma)
    /// - Quote character: `"` (double quotes)
    /// - Comment strategy: [CommentStrategy#NONE] (as comments are not part of RFC 4180)
    /// - Comment character: `#` (hash) (in case comment strategy is enabled)
    /// - Allow extra characters after closing quotes: `false`
    /// - Max buffer size: {@value %,2d #DEFAULT_MAX_BUFFER_SIZE} characters
    ///
    /// CR, LF and CRLF are all recognized as line breaks, even mixed within the same CSV data;
    /// this is not configurable.
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class IndexedCsvReaderBuilder {

        private static final int DEFAULT_MAX_BUFFER_SIZE = 16 * 1024 * 1024;

        private static final int DEFAULT_PAGE_SIZE = 100;
        private static final int MIN_PAGE_SIZE = 1;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean allowExtraCharsAfterClosingQuote;
        private boolean allowUnclosedQuote = true;

        @Nullable
        private StatusListener statusListener;

        private int pageSize = DEFAULT_PAGE_SIZE;

        @Nullable
        private CsvIndex csvIndex;

        private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;

        private IndexedCsvReaderBuilder() {
        }

        /// Sets the `fieldSeparator` used when reading CSV data.
        ///
        /// @param fieldSeparator the field separator character (default: `,` - comma).
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder fieldSeparator(final char fieldSeparator) {
            checkControlCharacter(fieldSeparator);
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /// Sets the `quoteCharacter` used when reading CSV data.
        ///
        /// @param quoteCharacter the quote character (default: `"` - double quotes).
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            checkControlCharacter(quoteCharacter);
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /// Sets the strategy for handling comments
        /// (default: [CommentStrategy#NONE] as comments are not part of RFC 4180).
        ///
        /// @param commentStrategy the strategy for handling comments.
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if [CommentStrategy#SKIP] is passed, as this is not supported
        /// @see #commentCharacter(char)
        public IndexedCsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
            Preconditions.checkArgument(commentStrategy != CommentStrategy.SKIP,
                "CommentStrategy SKIP is not supported in IndexedCsvReader");
            this.commentStrategy = commentStrategy;
            return this;
        }

        /// Sets the `commentCharacter` used when reading CSV data.
        ///
        /// @param commentCharacter the character that begins a comment (default: `#` - hash)
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @see #commentStrategy(CommentStrategy)
        public IndexedCsvReaderBuilder commentCharacter(final char commentCharacter) {
            checkControlCharacter(commentCharacter);
            this.commentCharacter = commentCharacter;
            return this;
        }

        /// Specifies whether the presence of characters between a closing quote character and the next field
        /// separator, line break or the end of the CSV data should be treated as an error or not.
        ///
        /// Example: `"a"b,"c"`
        ///
        /// If this is set to `true`, the first field is returned as `ab`.
        ///
        /// If this is set to `false`, a [CsvParseException] will be thrown.
        ///
        /// @param allowExtraCharsAfterClosingQuote allow extra characters after closing quotes (default: `false`).
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @see CsvReader.CsvReaderBuilder#trimWhitespacesAroundQuotes(boolean)
        /// @deprecated This option permits non-conforming input (RFC 4180 does not allow any character between
        ///     a closing quote character and the next field separator or line break) and yields unspecified
        ///     results if the extra characters contain quote characters. For sequential reading,
        ///     [CsvReader.CsvReaderBuilder#trimWhitespacesAroundQuotes(boolean)] provides a saner alternative.
        @Deprecated(forRemoval = true)
        public IndexedCsvReaderBuilder allowExtraCharsAfterClosingQuote(
            final boolean allowExtraCharsAfterClosingQuote) {
            this.allowExtraCharsAfterClosingQuote = allowExtraCharsAfterClosingQuote;
            return this;
        }

        /// Defines whether the CSV data may end inside a quoted field.
        ///
        /// Example: `"foo,bar`
        ///
        /// If this is set to `true`, the field content `foo,bar` is returned as a single field; otherwise,
        /// a [CsvParseException] will be thrown.
        ///
        /// Independent of this flag, a [CsvParseException] is thrown if the unclosed region exceeds
        /// [#maxBufferSize(int)].
        ///
        /// **The default will change to `false` in version 5.0.**
        ///
        /// @param allowUnclosedQuote tolerate an unclosed quoted field (default: `true`).
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder allowUnclosedQuote(final boolean allowUnclosedQuote) {
            this.allowUnclosedQuote = allowUnclosedQuote;
            return this;
        }

        /// Sets the `statusListener` to listen for indexer status updates.
        ///
        /// @param statusListener the status listener.
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder statusListener(final StatusListener statusListener) {
            this.statusListener = statusListener;
            return this;
        }

        /// Sets a prebuilt index that should be used for accessing the file.
        ///
        /// The file and all settings – including [#pageSize(int)] – have to be the ones the index was
        /// built with; otherwise an [IllegalArgumentException] is thrown.
        ///
        /// @param csvIndex a prebuilt index
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder index(final CsvIndex csvIndex) {
            this.csvIndex = csvIndex;
            return this;
        }

        /// Sets the `pageSize` for pages returned by [#readPage(int)]
        /// (default: [#DEFAULT_PAGE_SIZE]).
        ///
        /// One index entry (roughly 40 bytes of heap) is kept per `pageSize` records – a smaller page size
        /// grants finer-grained random access at the cost of memory.
        ///
        /// @param pageSize the maximum size of pages.
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder pageSize(final int pageSize) {
            Preconditions.checkArgument(pageSize >= MIN_PAGE_SIZE, () ->
                "pageSize must be >= %d".formatted(MIN_PAGE_SIZE));
            this.pageSize = pageSize;
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
        public IndexedCsvReaderBuilder maxBufferSize(final int maxBufferSize) {
            Preconditions.checkArgument(maxBufferSize > 0, "maxBufferSize must be greater than 0");
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /*
         * Characters from 0 to 127 are base ASCII and collision-free with UTF-8.
         * Characters from 128 to 255 need to be represented as a multibyte string in UTF-8.
         * Multibyte handling of control characters is currently not supported by the byte-oriented CSV indexer
         * of IndexedCsvReader.
         */
        private static void checkControlCharacter(final char controlChar) {
            Preconditions.checkArgument(!Util.isNewline(controlChar),
                "A newline character must not be used as control character");
            Preconditions.checkArgument(controlChar <= MAX_BASE_ASCII, () ->
                "Multibyte control characters are not supported in IndexedCsvReader: '%s' (value: %d)".formatted(
                controlChar, (int) controlChar));
        }

        /// Constructs a new [IndexedCsvReader] of [CsvRecord] for the specified path using UTF-8
        /// as the character set.
        ///
        /// Convenience method for [#build(CsvCallbackHandler,Path,Charset)] with
        /// [CsvRecordHandler] as the callback handler and
        /// [StandardCharsets#UTF_8] as the charset.
        ///
        /// @param file the file to read data from.
        /// @return a new IndexedCsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file or charset is `null`
        public IndexedCsvReader<CsvRecord> ofCsvRecord(final Path file) throws IOException {
            return build(CsvRecordHandler.of(), file, StandardCharsets.UTF_8);
        }

        /// Constructs a new [IndexedCsvReader] of [CsvRecord] for the specified arguments.
        ///
        /// Convenience method for [#build(CsvCallbackHandler,Path,Charset)] with
        /// [CsvRecordHandler] as the callback handler.
        ///
        /// @param file    the file to read data from.
        /// @param charset the character set to use.
        /// @return a new IndexedCsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if file or charset is `null`
        public IndexedCsvReader<CsvRecord> ofCsvRecord(final Path file, final Charset charset) throws IOException {
            return build(CsvRecordHandler.of(), file, charset);
        }

        /// Constructs a new [IndexedCsvReader] for the specified callback handler and path using UTF-8
        /// as the character set.
        ///
        /// Convenience method for [#build(CsvCallbackHandler,Path,Charset)] with [StandardCharsets#UTF_8]
        /// as charset.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the callback handler to use.
        /// @param file            the file to read data from.
        /// @return a new IndexedCsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if callbackHandler, file or charset is `null`
        public <T> IndexedCsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Path file)
            throws IOException {
            return build(callbackHandler, file, StandardCharsets.UTF_8);
        }

        /// Constructs a new [IndexedCsvReader] for the specified arguments.
        ///
        /// Only UTF-8 and single-byte charsets are supported – whether passed explicitly or detected via a
        /// BOM. Every configured control character must additionally map to its own single byte in
        /// both directions under that charset.
        ///
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the callback handler to use.
        /// @param file            the file to read data from.
        /// @param charset         the character set to use (UTF-8 or a single-byte charset).
        /// @return a new IndexedCsvReader - never `null`. Remember to close it!
        /// @throws IOException              if an I/O error occurs.
        /// @throws NullPointerException     if callbackHandler, file or charset is `null`
        /// @throws IllegalArgumentException if argument validation fails, the (supplied or BOM-detected)
        ///                                  charset is neither UTF-8 nor single-byte, or a configured
        ///                                  control character does not map to its own byte in it.
        public <T> IndexedCsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                             final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final var sl = statusListener != null ? statusListener
                : new StatusListener() { };

            return new IndexedCsvReader<>(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, allowExtraCharsAfterClosingQuote, allowUnclosedQuote,
                maxBufferSize, pageSize, callbackHandler, csvIndex, sl);
        }

    }

    private final class ScannerListener implements CsvScanner.CsvListener {

        private final StatusListener statusListener;
        private final List<CsvIndex.CsvPage> pageOffsets = new ArrayList<>();
        private final AtomicLong recordCounter = new AtomicLong();
        private final AtomicLong startingLineNumber = new AtomicLong(1);

        private ScannerListener(final StatusListener statusListener) {
            this.statusListener = statusListener;
        }

        @Override
        public void onReadBytes(final int readCnt) {
            statusListener.onReadBytes(readCnt);
        }

        @Override
        public void startOffset(final long offset) {
            if (recordCounter.getAndIncrement() % pageSize == 0) {
                pageOffsets.add(new CsvIndex.CsvPage(offset, startingLineNumber.get()));
            }
        }

        @Override
        public void onReadRecord() {
            startingLineNumber.incrementAndGet();
            statusListener.onReadRecord();
        }

        @Override
        public void additionalLine() {
            startingLineNumber.incrementAndGet();
        }

    }

}
