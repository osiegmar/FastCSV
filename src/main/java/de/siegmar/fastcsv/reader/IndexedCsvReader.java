package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CSV reader implementation for indexed based access.
 * <p>
 * Right after instantiating, this class scans the given file for CSV row positions in background.
 * This process is optimized on performance and low memory usage – no CSV data is stored in memory.
 * The current status can be monitored via {@link IndexedCsvReaderBuilder#statusListener(StatusListener)}.
 * <p>
 * This class is thread-safe.
 * <p>
 * Example use:
 * <pre>{@code
 * try (IndexedCsvReader csv = IndexedCsvReader.builder().build(file)) {
 *     List<CsvRow> rows = csvReader.readPage(10).get();
 * }
 * }</pre>
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public final class IndexedCsvReader implements Closeable {

    private final List<Long> pageOffsets = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong rowCounter = new AtomicLong();

    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final int pageSize;
    private final RandomAccessFile raf;
    private final RowReader rowReader;
    private final CompletableFuture<Void> scanner;

    @SuppressWarnings("checkstyle:ParameterNumber")
    IndexedCsvReader(final Path file, final Charset charset,
                     final char fieldSeparator, final char quoteCharacter,
                     final CommentStrategy commentStrategy, final char commentCharacter,
                     final int pageSize, final StatusListener statusListener) throws IOException {

        if (fieldSeparator == quoteCharacter || fieldSeparator == commentCharacter
            || quoteCharacter == commentCharacter) {
            throw new IllegalArgumentException(String.format("Control characters must differ"
                    + " (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
                fieldSeparator, quoteCharacter, commentCharacter));
        }

        this.file = file;
        this.charset = charset;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.pageSize = pageSize;

        raf = new RandomAccessFile(file.toFile(), "r");
        rowReader = new RowReader(new InputStreamReader(new RandomAccessFileInputStream(raf), charset),
            fieldSeparator, quoteCharacter, commentStrategy, commentCharacter);

        scanner = CompletableFuture
            .runAsync(() -> scan(statusListener))
            .whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    statusListener.onError(throwable);
                } else {
                    statusListener.onComplete();
                }
            });
    }

    private void scan(final StatusListener statusListener) {
        try (var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            statusListener.onInit(channel.size());

            new CsvScanner(channel, (byte) fieldSeparator, (byte) quoteCharacter,
                commentStrategy, (byte) commentCharacter, this::collectOffset, statusListener).scan();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("checkstyle:ParameterAssignment")
    private void collectOffset(final long offset) {
        if (rowCounter.getAndIncrement() % pageSize == 0) {
            pageOffsets.add(offset);
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
     * Gets the {@link CompletableFuture} that represents the background indexing process.
     *
     * @return the {@link CompletableFuture} that represents the background indexing process.
     */
    public CompletableFuture<Void> completableFuture() {
        return CompletableFuture.allOf(scanner);
    }

    /**
     * Gets the number of pages the file contents is partitioned to.
     *
     * @return the number of pages the file contents is partitioned to
     */
    public CompletableFuture<Integer> pageCount() {
        return scanner.thenApply(unused -> pageOffsets.size());
    }

    /**
     * Gets the number of rows the file contains.
     *
     * @return the number of rows the file contains
     */
    public CompletableFuture<Long> rowCount() {
        return scanner.thenApply(unused -> rowCounter.get());
    }

    /**
     * Reads a page of rows, returning a {@link CompletableFuture} to allow non-blocking read.
     *
     * @param page the page to read (0-based).
     * @return a Java Future of Stream of {@link List} that when completed contains the requested rows,
     *     never {@code null}.
     *     The CompletableFuture returned by this method can be completed exceptionally with an
     *     {@link IndexOutOfBoundsException} if the file does not contain the specified page.
     * @throws IllegalArgumentException if {@code page} is &lt; 0
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public CompletableFuture<List<CsvRow>> readPage(final int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }

        return findFirstRowOffsetOfPage(page)
            .thenApply(offset -> {
                final List<CsvRow> ret = new ArrayList<>(pageSize);
                synchronized (rowReader) {
                    try {
                        raf.seek(offset);
                        rowReader.resetBuffer((pageSize * page) + 1);

                        CsvRow csvRow;
                        for (int i = 0; i < pageSize && (csvRow = rowReader.fetchAndRead()) != null; i++) {
                            ret.add(csvRow);
                        }

                        return ret;
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
    }

    private CompletableFuture<Long> findFirstRowOffsetOfPage(final int page) {
        return waitForPage(page)
            .thenApply(unused -> pageOffsets.get(page));
    }

    private CompletableFuture<Void> waitForPage(final int page) {
        return CompletableFuture.runAsync(() -> {
            while (page >= pageOffsets.size() && !scanner.isDone()) {
                Thread.onSpinWait();
            }
        });
    }

    /**
     * Interrupts the scanning process (if still running) and closes the file.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        scanner.cancel(true);
        raf.close();
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
        private StatusListener statusListener;
        private int pageSize = DEFAULT_PAGE_SIZE;

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
            if (commentStrategy == CommentStrategy.SKIP) {
                throw new IllegalArgumentException("CommentStrategy SKIP is not supported in IndexedCsvReader");
            }
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
         * Sets the {@code pageSize} for pages returned by {@link IndexedCsvReader#readPage(int)}.
         *
         * @param pageSize the maximum size of pages.
         * @return This updated object, so that additional method calls can be chained together.
         */
        public IndexedCsvReaderBuilder pageSize(final int pageSize) {
            if (pageSize < MIN_PAGE_SIZE) {
                throw new IllegalArgumentException("pageSize must be > 0");
            }
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
            if (controlChar > MAX_BASE_ASCII) {
                throw new IllegalArgumentException(String.format(
                    "Multibyte control characters are not supported in IndexedCsvReader: '%s' (value: %d)",
                    controlChar, (int) controlChar));
            } else if (controlChar == CR || controlChar == LF) {
                throw new IllegalArgumentException("A newline character must not be used as control character");
            }
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

            final StatusListener sl = Objects
                .requireNonNullElseGet(statusListener, () -> new StatusListener() {
                });

            return new IndexedCsvReader(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, pageSize, sl);
        }

    }

}
