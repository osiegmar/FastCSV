package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.containsDupe;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;

import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/// CSV reader implementation for indexed based access.
///
/// If no prebuilt index passed in (via [IndexedCsvReaderBuilder#index(CsvIndex)]) the constructor will initiate
/// indexing the file.
/// This process is optimized on performance and low memory usage – no CSV data is stored in memory.
/// The current status can be monitored via [IndexedCsvReaderBuilder#statusListener(StatusListener)].
///
/// This class is thread-safe.
///
/// Example use:
/// ```
/// try (IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder().ofCsvRecord(file)) {
///     CsvIndex index = csv.getIndex();
///     int lastPage = index.getPageCount() - 1;
///     List<CsvRecord> csvRecords = csv.readPage(lastPage);
/// }
/// ```
///
/// @param <T> the type of the CSV record.
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public final class IndexedCsvReader<T> implements Closeable {

    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final boolean acceptCharsAfterQuotes;
    private final int pageSize;
    private final RandomAccessFile raf;
    private final CsvCallbackHandler<T> csvRecordHandler;
    private final CsvParser csvParser;
    private final CsvIndex csvIndex;

    @SuppressWarnings("checkstyle:ParameterNumber")
    IndexedCsvReader(final Path file, final Charset defaultCharset,
                     final char fieldSeparator, final char quoteCharacter,
                     final CommentStrategy commentStrategy, final char commentCharacter,
                     final boolean acceptCharsAfterQuotes, final int pageSize,
                     final CsvCallbackHandler<T> csvRecordHandler, final CsvIndex csvIndex,
                     final StatusListener statusListener)
        throws IOException {

        Preconditions.checkArgument(!containsDupe(fieldSeparator, quoteCharacter, commentCharacter),
            "Control characters must differ"
                + " (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
            fieldSeparator, quoteCharacter, commentCharacter);

        this.file = file;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
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

        if (csvIndex != null) {
            this.csvIndex = validatePrebuiltIndex(file, bomHeaderLength,
                (byte) fieldSeparator, (byte) quoteCharacter, commentStrategy, (byte) commentCharacter,
                csvIndex);
        } else {
            this.csvIndex = buildIndex(bomHeaderLength, statusListener);
        }

        raf = new RandomAccessFile(file.toFile(), "r");
        csvParser = new CsvParser(fieldSeparator, quoteCharacter, commentStrategy, commentCharacter,
            acceptCharsAfterQuotes, csvRecordHandler,
            new InputStreamReader(new RandomAccessFileInputStream(raf), charset));
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

    private static CsvIndex validatePrebuiltIndex(final Path file, final int bomHeaderLength, final byte fieldSeparator,
                                                  final byte quoteCharacter, final CommentStrategy commentStrategy,
                                                  final byte commentCharacter, final CsvIndex csvIndex)
        throws IOException {
        final var expectedSignature = new StringJoiner(", ")
            .add("bomHeaderLength=" + bomHeaderLength)
            .add("fileSize=" + Files.size(file))
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .toString();
        final var actualSignature = new StringJoiner(", ")
            .add("bomHeaderLength=" + csvIndex.getBomHeaderLength())
            .add("fileSize=" + csvIndex.getFileSize())
            .add("fieldSeparator=" + csvIndex.getFieldSeparator())
            .add("quoteCharacter=" + csvIndex.getQuoteCharacter())
            .add("commentStrategy=" + csvIndex.getCommentStrategy())
            .add("commentCharacter=" + csvIndex.getCommentCharacter())
            .toString();

        Preconditions.checkArgument(expectedSignature.equals(actualSignature),
            "Index does not match! Expected: %s; Actual: %s",
            expectedSignature, actualSignature);

        return csvIndex;
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingThrowable"})
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
                commentStrategy, (byte) commentCharacter,
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
        return readPage(csvIndex.getPage(page));
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private List<T> readPage(final CsvIndex.CsvPage page) throws IOException {
        final List<T> ret = new ArrayList<>(pageSize);
        synchronized (raf) {
            raf.seek(page.getOffset());
            csvParser.reset(page.getStartingLineNumber() - 1);

            for (int i = 0; i < pageSize && csvParser.parse(); i++) {
                final RecordWrapper<T> rec = csvRecordHandler.buildRecord();
                if (rec != null) {
                    ret.add(rec.getWrappedRecord());
                }
            }

            return ret;
        }
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
            .add("acceptCharsAfterQuotes=" + acceptCharsAfterQuotes)
            .add("pageSize=" + pageSize)
            .add("index=" + csvIndex)
            .toString();
    }

    /// This builder is used to create configured instances of [IndexedCsvReader]. The default
    /// configuration of this class adheres with RFC 4180.
    ///
    /// The line delimiter (line-feed, carriage-return or the combination of both) is detected
    /// automatically and thus not configurable.
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class IndexedCsvReaderBuilder {

        private static final int MAX_BASE_ASCII = 127;
        private static final int DEFAULT_PAGE_SIZE = 100;
        private static final int MIN_PAGE_SIZE = 1;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean acceptCharsAfterQuotes = true;
        private StatusListener statusListener;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private CsvIndex csvIndex;

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
        /// @param quoteCharacter the character used to enclose fields
        ///                       (default: `"` - double quotes).
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            checkControlCharacter(quoteCharacter);
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /// Sets the strategy that defines how (and if) commented lines should be handled
        /// (default: [CommentStrategy#NONE] as comments are not defined in RFC 4180).
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

        /// Sets the `commentCharacter` used to comment lines.
        ///
        /// @param commentCharacter the character used to comment lines (default: `#` - hash)
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @see #commentStrategy(CommentStrategy)
        public IndexedCsvReaderBuilder commentCharacter(final char commentCharacter) {
            checkControlCharacter(commentCharacter);
            this.commentCharacter = commentCharacter;
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
        public IndexedCsvReaderBuilder acceptCharsAfterQuotes(final boolean acceptCharsAfterQuotes) {
            this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
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
        /// @param csvIndex a prebuilt index
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder index(final CsvIndex csvIndex) {
            this.csvIndex = csvIndex;
            return this;
        }

        /// Sets the `pageSize` for pages returned by [#readPage(int)]
        /// (default: [#DEFAULT_PAGE_SIZE]).
        ///
        /// @param pageSize the maximum size of pages.
        /// @return This updated object, allowing additional method calls to be chained together.
        public IndexedCsvReaderBuilder pageSize(final int pageSize) {
            Preconditions.checkArgument(pageSize >= MIN_PAGE_SIZE,
                "pageSize must be >= %d", MIN_PAGE_SIZE);
            this.pageSize = pageSize;
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
            Preconditions.checkArgument(controlChar <= MAX_BASE_ASCII,
                "Multibyte control characters are not supported in IndexedCsvReader: '%s' (value: %d)",
                controlChar, (int) controlChar);
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
            return build(new CsvRecordHandler(), file, StandardCharsets.UTF_8);
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
            return build(new CsvRecordHandler(), file, charset);
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
        /// @param <T>             the type of the CSV record.
        /// @param callbackHandler the callback handler to use.
        /// @param file            the file to read data from.
        /// @param charset         the character set to use.
        /// @return a new IndexedCsvReader - never `null`. Remember to close it!
        /// @throws IOException          if an I/O error occurs.
        /// @throws NullPointerException if callbackHandler, file or charset is `null`
        public <T> IndexedCsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                             final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final var sl = statusListener != null ? statusListener
                : new StatusListener() { };

            return new IndexedCsvReader<>(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, acceptCharsAfterQuotes, pageSize, callbackHandler, csvIndex, sl);
        }

    }

    private final class ScannerListener implements CsvScanner.CsvListener {

        private final StatusListener statusListener;
        private final List<CsvIndex.CsvPage> pageOffsets = new ArrayList<>();
        private final AtomicLong recordCounter = new AtomicLong();
        private long startingLineNumber = 1;

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
                pageOffsets.add(new CsvIndex.CsvPage(offset, startingLineNumber));
            }
        }

        @Override
        public void onReadRecord() {
            startingLineNumber++;
            statusListener.onReadRecord();
        }

        @Override
        public void additionalLine() {
            startingLineNumber++;
        }

    }

}
