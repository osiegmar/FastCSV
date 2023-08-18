package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.ReadableByteChannel;
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
import java.util.stream.Stream;

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
 *     CsvRow row = csvReader.readRow(3000).get();
 * }
 * }</pre>
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public final class IndexedCsvReader implements Closeable {

    private final List<Integer> positions = Collections.synchronizedList(new ArrayList<>());
    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final StatusListener statusListener;

    private final StatusConsumerImpl statusConsumer;
    private final CompletableFuture<Void> scanner;
    private final RandomAccessFile raf;
    private final RowReader rowReader;

    IndexedCsvReader(final Path file, final Charset charset,
                     final char fieldSeparator, final char quoteCharacter,
                     final CommentStrategy commentStrategy, final char commentCharacter,
                     final StatusListener statusListener) throws IOException {

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
        this.statusListener = statusListener;

        statusConsumer = new StatusConsumerImpl();

        statusListener.initialize(Files.size(file));

        scanner = CompletableFuture.runAsync(() -> {
            try (ReadableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
                new CsvScanner(channel, (byte) fieldSeparator, (byte) quoteCharacter,
                    commentStrategy, (byte) commentCharacter, statusConsumer).scan();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                statusListener.failed(throwable);
            } else {
                statusListener.completed();
            }
        });

        raf = new RandomAccessFile(file.toFile(), "r");
        rowReader = new RowReader(new InputStreamReader(new ChannelInputStream(raf), charset),
            fieldSeparator, quoteCharacter, commentStrategy, commentCharacter);
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
     * Gets the number of rows the file contains.
     *
     * @return the number of rows the file contains
     */
    public CompletableFuture<Integer> size() {
        return scanner.thenApply(unused -> positions.size());
    }

    /**
     * Reads a CSV row by the given row number, returning a {@link CompletableFuture} to
     * allow non-blocking read. The result will be available when the requested row has been read.
     *
     * @param rowNum the row number (0-based) to read from
     * @return a {@link CsvRow} fetched from the specified {@code rowNum}
     * @throws IllegalArgumentException if specified {@code rowNum} is lower than 0
     */
    public CompletableFuture<CsvRow> readRow(final int rowNum) {
        if (rowNum < 0) {
            throw new IllegalArgumentException("Row# must be >= 0");
        }

        return getOffset(rowNum).thenApply(offset -> {
            synchronized (rowReader) {
                try {
                    seek(rowNum, offset);
                    final CsvRow csvRow = rowReader.fetchAndRead();
                    if (csvRow == null) {
                        throw new ArrayIndexOutOfBoundsException("No data found at rowNum# " + rowNum);
                    }
                    return csvRow;
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    /**
     * Reads a number of rows, returning a {@link CompletableFuture} to
     * allow non-blocking read. The result will be available when the requested rows have been read.
     *
     * @param firstRow the first row to read from (0-based).
     * @param maxRows  the maximum number of rows to read.
     * @return up to {@code maxRows} beginning from {@code firstRow}
     * @throws IllegalArgumentException if {@code firstRow} is &lt; 0 or {@code maxRows} &le; 0
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public CompletableFuture<Stream<CsvRow>> readRows(final int firstRow, final int maxRows) {
        if (firstRow < 0) {
            throw new IllegalArgumentException("firstRow must be >= 0");
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be > 0");
        }

        return getOffset(firstRow)
            .thenApply(offset -> {
                final Stream.Builder<CsvRow> ret = Stream.builder();
                synchronized (rowReader) {
                    try {
                        seek(firstRow, offset);

                        CsvRow csvRow;
                        for (int i = 0; i < maxRows && (csvRow = rowReader.fetchAndRead()) != null; i++) {
                            ret.accept(csvRow);
                        }

                        return ret.build();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
    }

    private void seek(final int row, final int offset) throws IOException {
        rowReader.resetBuffer(row + 1);
        raf.seek(offset);
    }

    private CompletableFuture<Integer> getOffset(final int row) {
        if (row == 0) {
            return CompletableFuture.completedFuture(0);
        }

        return waitForRow(row)
            .thenApply(unused -> positions.get(row));
    }

    private CompletableFuture<Void> waitForRow(final int row) {
        return CompletableFuture.runAsync(() -> {
            while (positions.size() < row && !scanner.isDone()) {
                Thread.onSpinWait();
            }
        });
    }

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

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private StatusListener statusListener;

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
         * @see #commentCharacter(char)
         */
        public IndexedCsvReaderBuilder commentStrategy(
            final CommentStrategy commentStrategy) {
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
            } else if (controlChar == '\r' || controlChar == '\n') {
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
                .requireNonNullElseGet(statusListener, () -> new StatusListener() { });

            return new IndexedCsvReader(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, sl);
        }

    }

    private class StatusConsumerImpl implements StatusConsumer {

        @Override
        public void addRowPosition(final int position) {
            positions.add(position);
            statusListener.readRow();
        }

        @Override
        public void addReadBytes(final int readCnt) {
            statusListener.readBytes(readCnt);
        }

    }

    private static class ChannelInputStream extends InputStream {

        private final RandomAccessFile raf;

        ChannelInputStream(final RandomAccessFile raf) {
            this.raf = raf;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return raf.read(b, off, len);
        }

    }

}
