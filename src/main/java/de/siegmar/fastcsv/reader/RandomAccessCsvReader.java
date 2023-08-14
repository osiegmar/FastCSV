package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * CSV reader implementation for random access.
 * <p>
 * Right after instantiating, this class scans the given file for CSV record positions in background.
 * This process is optimized on performance and low memory usage – no CSV data is stored in memory.
 * The current status can be monitored via {@link #getStatusMonitor()}.
 * <p>
 * This class is thread-safe.
 * <p>
 * Example use:
 * <pre>{@code
 * try (RandomAccessCsvReader csvReader = RandomAccessCsvReader.builder().build(file)) {
 *     CsvRow row = csvReader.readRow(3000)
 *         get(1, TimeUnit.SECONDS);
 *     System.out.println(row);
 * }
 * }</pre>
 */
public final class RandomAccessCsvReader implements Closeable {

    private final List<Integer> positions = Collections.synchronizedList(new ArrayList<>());
    private final StatusMonitorImpl statusMonitor = new StatusMonitorImpl();
    private final Path file;
    private final Charset charset;
    private final char fieldSeparator;
    private final char quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final char commentCharacter;
    private final CompletableFuture<Void> scanner;
    private final RandomAccessFile raf;
    private final RowReader rowReader;

    RandomAccessCsvReader(final Path file, final Charset charset,
                          final char fieldSeparator, final char quoteCharacter,
                          final CommentStrategy commentStrategy, final char commentCharacter) throws IOException {

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

        scanner = CompletableFuture.runAsync(() -> {
            try (ReadableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
                CsvScanner.scan(channel, (byte) quoteCharacter, statusMonitor);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
        });

        raf = new RandomAccessFile(file.toFile(), "r");
        rowReader = new RowReader(new InputStreamReader(new ChannelInputStream(raf), charset),
            fieldSeparator, quoteCharacter, commentStrategy, commentCharacter);
    }

    /**
     * Constructs a {@link RandomAccessCsvReader.RandomAccessCsvReaderBuilder} to configure and build instances of
     * this class.
     *
     * @return a new {@link RandomAccessCsvReader.RandomAccessCsvReaderBuilder} instance.
     */
    public static RandomAccessCsvReaderBuilder builder() {
        return new RandomAccessCsvReaderBuilder();
    }

    public StatusMonitor getStatusMonitor() {
        return statusMonitor;
    }

    public void awaitIndex() throws ExecutionException, InterruptedException {
        scanner.get();
    }

    public boolean awaitIndex(final long timeout, final TimeUnit unit) throws ExecutionException, InterruptedException {
        try {
            scanner.get(timeout, unit);
        } catch (final TimeoutException e) {
            return false;
        }

        return true;
    }

    public CompletableFuture<Integer> size() {
        return scanner.thenApply(unused -> positions.size());
    }

    /**
     * Reads a CSV row by the given row number (0-based), returning a {@link CompletableFuture} to
     * allow non-blocking read.
     *
     * @param rowNum the row number (0-based) to read from
     * @return a {@link CsvRow} fetched from the specified {@code rowNum}
     * @throws IllegalArgumentException if specified {@code rowNum} is lower than 0
     */
    public CompletableFuture<CsvRow> readRow(final int rowNum) {
        if (rowNum < 0) {
            throw new IllegalArgumentException("Record# must be >= 0");
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
                } catch (final Exception e) {
                    throw new CompletionException(e);
                }
            }
        });
    }

    public <CONSUMER extends Consumer<CsvRow>> CompletableFuture<CONSUMER> readRows(final int firstRecord, final int maxRecords, final CONSUMER consumer) {
        if (firstRecord < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (maxRecords <= 0) {
            throw new IllegalArgumentException();
        }

        Objects.requireNonNull(consumer);

        return getOffset(firstRecord)
            .thenAccept(offset -> {
                synchronized (rowReader) {
                    try {
                        seek(firstRecord, offset);

                        CsvRow csvRow;
                        for (int i = 0; i < maxRecords && (csvRow = rowReader.fetchAndRead()) != null; i++) {
                            consumer.accept(csvRow);
                        }
                    } catch (final IOException e) {
                        throw new CompletionException(e);
                    }
                }
            })
            .thenApply(unused -> consumer);
    }

    private void seek(final int record, final int offset) throws IOException {
        rowReader.resetBuffer(record + 1);
        raf.seek(offset);
    }

    private CompletableFuture<Integer> getOffset(final int record) {
        if (record == 0) {
            return CompletableFuture.completedFuture(0);
        }

        return waitForRecord(record)
            .thenApply(unused -> positions.get(record));
    }

    private CompletableFuture<Void> waitForRecord(final int record) {
        return CompletableFuture.runAsync(() -> {
            while (positions.size() < record && !scanner.isDone()) {
                try {
                    scanner.get(100, TimeUnit.MILLISECONDS);
                } catch (final TimeoutException ignored) {
                    // ignore
                } catch (final ExecutionException | InterruptedException e) {
                    throw new CompletionException("Exception while waiting for scanner result", e);
                }
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
        return new StringJoiner(", ", RandomAccessCsvReader.class.getSimpleName() + "[", "]")
            .add("file=" + file)
            .add("charset=" + charset)
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .toString();
    }

    public static final class RandomAccessCsvReaderBuilder {

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';

        public RandomAccessCsvReaderBuilder() {
        }

        /**
         * Sets the {@code fieldSeparator} used when reading CSV data.
         *
         * @param fieldSeparator the field separator character (default: {@code ,} - comma).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public RandomAccessCsvReader.RandomAccessCsvReaderBuilder fieldSeparator(final char fieldSeparator) {
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
        public RandomAccessCsvReader.RandomAccessCsvReaderBuilder quoteCharacter(final char quoteCharacter) {
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
        public RandomAccessCsvReader.RandomAccessCsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
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
        public RandomAccessCsvReader.RandomAccessCsvReaderBuilder commentCharacter(final char commentCharacter) {
            checkControlCharacter(commentCharacter);
            this.commentCharacter = commentCharacter;
            return this;
        }

        /*
         * Characters from 0 to 127 are base ASCII and collision-free with UTF-8.
         * Characters from 128 to 255 needs to be represented as a multibyte string in UTF-8.
         * Multibyte handling of control characters is currently not supported by the byte-oriented CSV indexer
         * of RandomAccessCsvReader.
         */
        private static void checkControlCharacter(final char controlChar) {
            if (controlChar > 127) {
                throw new IllegalArgumentException(String.format(
                    "Multibyte control characters are not supported in RandomAccessCsvReader: '%s' (value: %d)",
                    controlChar, (int) controlChar));
            } else if (controlChar == '\r' || controlChar == '\n') {
                throw new IllegalArgumentException("A newline character must not be used as control character");
            }
        }

        public RandomAccessCsvReader build(final Path file) throws IOException {
            return build(file, StandardCharsets.UTF_8);
        }

        public RandomAccessCsvReader build(final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return new RandomAccessCsvReader(file, charset, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter);
        }

    }

    private class StatusMonitorImpl implements StatusConsumer, StatusMonitor {

        private final AtomicLong positionCount = new AtomicLong();
        private final AtomicLong readBytes = new AtomicLong();

        @Override
        public void addPosition(final int position) {
            positions.add(position);
            positionCount.incrementAndGet();
        }

        @Override
        public long getPositionCount() {
            return positionCount.get();
        }

        @Override
        public void addReadBytes(final int readCnt) {
            readBytes.addAndGet(readCnt);
        }

        @Override
        public long getReadBytes() {
            return readBytes.get();
        }

        @Override
        public String toString() {
            return String.format("Read %,d bytes / %,d lines", readBytes.get(), positionCount.get());
        }

    }

    private static class ChannelInputStream extends InputStream {

        private final RandomAccessFile raf;

        public ChannelInputStream(final RandomAccessFile raf) {
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
