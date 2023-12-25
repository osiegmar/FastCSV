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

/**
 * CSV reader implementation for indexed based access.
 * <p>
 * If no prebuild index passed in (via {@link IndexedCsvReaderBuilder#index(CsvIndex)}) the constructor will initiate
 * indexing the file.
 * This process is optimized on performance and low memory usage – no CSV data is stored in memory.
 * The current status can be monitored via {@link IndexedCsvReaderBuilder#statusListener(StatusListener)}.
 * <p>
 * This class is thread-safe.
 * <p>
 * Example use:
 * {@snippet :
 * try (IndexedCsvReader csv = IndexedCsvReader.builder().build(file)) {
 *     CsvIndex index = csv.index();
 *     int lastPage = index.pageCount() - 1;
 *     List<CsvRecord> csvRecords = csv.readPage(lastPage);
 * }
 *}
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public final class IndexedCsvReader implements Closeable {

    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final int pageSize;
    private final RandomAccessFile raf;
    private final RecordHandler recordHandler;
    private final RecordReader recordReader;
    private final CsvIndex csvIndex;

    @SuppressWarnings("checkstyle:ParameterNumber")
    IndexedCsvReader(final Path file, final Charset defaultCharset,
                     final char fieldSeparator, final char quoteCharacter,
                     final CommentStrategy commentStrategy, final char commentCharacter,
                     final FieldModifier fieldModifier, final int pageSize, final CsvIndex csvIndex,
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
        recordHandler = new RecordHandler(fieldModifier);
        this.pageSize = pageSize;

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
        recordReader = new RecordReader(recordHandler,
            new InputStreamReader(new RandomAccessFileInputStream(raf), charset),
            fieldSeparator, quoteCharacter, commentStrategy, commentCharacter);
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

    /**
     * Constructs a {@link IndexedCsvReaderBuilder} to configure and build instances of
     * this class.
     *
     * @return a new {@link IndexedCsvReaderBuilder} instance.
     */
    public static IndexedCsvReaderBuilder builder() {
        return new IndexedCsvReaderBuilder();
    }

    /**
     * Obtain the index that is used for accessing the CSV file.
     * That index is either a freshly built index or the index that has been
     * passed via {@link IndexedCsvReaderBuilder#index(CsvIndex)}.
     *
     * @return the index that is used for accessing the CSV file.
     */
    public CsvIndex index() {
        return csvIndex;
    }

    /**
     * Reads a page of records.
     *
     * @param page the page to read (0-based).
     * @return a page of records, never {@code null}.
     * @throws IOException               if an I/O error occurs.
     * @throws IllegalArgumentException  if {@code page} is &lt; 0
     * @throws IndexOutOfBoundsException if the file does not contain the specified page
     */
    public List<CsvRecord> readPage(final int page) throws IOException {
        Preconditions.checkArgument(page >= 0, "page must be >= 0");
        return readPage(csvIndex.page(page));
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private List<CsvRecord> readPage(final CsvIndex.CsvPage page) throws IOException {
        final List<CsvRecord> ret = new ArrayList<>(pageSize);
        synchronized (raf) {
            raf.seek(page.offset());
            recordReader.resetBuffer(page.startingLineNumber());

            for (int i = 0; i < pageSize && recordReader.read(); i++) {
                ret.add(recordHandler.buildAndReset());
            }

            return ret;
        }
    }

    @Override
    public void close() throws IOException {
        recordReader.close();
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
            .add("pageSize=" + pageSize)
            .add("index=" + csvIndex)
            .toString();
    }

    /**
     * This builder is used to create configured instances of {@link IndexedCsvReader}. The default
     * configuration of this class complies with RFC 4180.
     * <p>
     * The line delimiter (line-feed, carriage-return or the combination of both) is detected
     * automatically and thus not configurable.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class IndexedCsvReaderBuilder {

        private static final int MAX_BASE_ASCII = 127;
        private static final int DEFAULT_PAGE_SIZE = 100;
        private static final int MIN_PAGE_SIZE = 1;

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private FieldModifier fieldModifier;
        private StatusListener statusListener;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private CsvIndex csvIndex;

        private IndexedCsvReaderBuilder() {
        }

        /**
         * Sets the {@code fieldSeparator} used when reading CSV data.
         *
         * @param fieldSeparator the field separator character (default: {@code ,} - comma).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder fieldSeparator(final char fieldSeparator) {
            checkControlCharacter(fieldSeparator);
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /**
         * Sets the {@code quoteCharacter} used when reading CSV data.
         *
         * @param quoteCharacter the character used to enclose fields
         *                       (default: {@code "} - double quotes).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            checkControlCharacter(quoteCharacter);
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines how (and if) commented lines should be handled
         * (default: {@link CommentStrategy#NONE} as comments are not defined in RFC 4180).
         *
         * @param commentStrategy the strategy for handling comments.
         * @return This updated object, so that additional method calls can be chained together.
         * @throws IllegalArgumentException if {@link CommentStrategy#SKIP} is passed, as this is not supported
         * @see #commentCharacter(char)
         */
        public IndexedCsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
            Preconditions.checkArgument(commentStrategy != CommentStrategy.SKIP,
                "CommentStrategy SKIP is not supported in IndexedCsvReader");
            this.commentStrategy = commentStrategy;
            return this;
        }

        /**
         * Sets the {@code commentCharacter} used to comment lines.
         *
         * @param commentCharacter the character used to comment lines (default: {@code #} - hash)
         * @return This updated object, so that additional method calls can be chained together.
         * @see #commentStrategy(CommentStrategy)
         */
        public IndexedCsvReaderBuilder commentCharacter(final char commentCharacter) {
            checkControlCharacter(commentCharacter);
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Registers an optional field modifier. Used to modify the field values.
         * By default, no field modifier is used.
         *
         * @param fieldModifier the modifier to use.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder fieldModifier(final FieldModifier fieldModifier) {
            this.fieldModifier = fieldModifier;
            return this;
        }

        /**
         * Sets the {@code statusListener} to listen for indexer status updates.
         *
         * @param statusListener the status listener.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder statusListener(final StatusListener statusListener) {
            this.statusListener = statusListener;
            return this;
        }

        /**
         * Sets a prebuilt index that should be used for accessing the file.
         *
         * @param csvIndex a prebuilt index
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder index(final CsvIndex csvIndex) {
            this.csvIndex = csvIndex;
            return this;
        }

        /**
         * Sets the {@code pageSize} for pages returned by {@link IndexedCsvReader#readPage(int)}
         * (default: {@value DEFAULT_PAGE_SIZE}).
         *
         * @param pageSize the maximum size of pages.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder pageSize(final int pageSize) {
            Preconditions.checkArgument(pageSize >= MIN_PAGE_SIZE,
                "pageSize must be >= %d", MIN_PAGE_SIZE);
            this.pageSize = pageSize;
            return this;
        }

        /*
         * Characters from 0 to 127 are base ASCII and collision-free with UTF-8.
         * Characters from 128 to 255 needs to be represented as a multibyte string in UTF-8.
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

        /**
         * Constructs a new {@link IndexedCsvReader} for the specified path using UTF-8 as the character set.
         *
         * @param file the file to read data from.
         * @return a new IndexedCsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public IndexedCsvReader build(final Path file) throws IOException {
            return build(file, StandardCharsets.UTF_8);
        }

        /**
         * Constructs a new {@link IndexedCsvReader} for the specified arguments.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use.
         * @return a new IndexedCsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public IndexedCsvReader build(final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final var sl = statusListener != null ? statusListener
                : new StatusListener() { };

            return new IndexedCsvReader(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, fieldModifier, pageSize, csvIndex, sl);
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
