package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class RandomAccessCsvReader implements Closeable {

    private final List<Integer> positions = Collections.synchronizedList(new ArrayList<>());
    private final StatusMonitorImpl statusMonitor = new StatusMonitorImpl();
    private final CompletableFuture<Void> scanner;
    private final RandomAccessFile raf;
    private final RowReader rowReader;

    RandomAccessCsvReader(final Path file, final Charset charset,
                          final char quoteCharacter, final char fieldSeparator,
                          final CommentStrategy commentStrategy, final char commentCharacter) throws IOException {

        scanner = CompletableFuture.runAsync(() -> {
            try {
                CsvScanner.scan(file, (byte) quoteCharacter, statusMonitor);
            } catch (final IOException e) {
                throw new CompletionException(e);
            }
        });

        raf = new RandomAccessFile(file.toFile(), "r");
        rowReader = new RowReader(new InputStreamReader(new ChannelInputStream(raf), charset),
            fieldSeparator, quoteCharacter, commentStrategy, commentCharacter);
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

    public CompletableFuture<CsvRow> read(final int record) {
        return getOffset(record).thenApply(offset -> {
            synchronized (rowReader) {
                try {
                    seek(record, offset);
                    return rowReader.fetchAndRead();
                } catch (final IOException e) {
                    throw new CompletionException(e);
                }
            }
        });
    }

//    public Iterator<CsvRow> iterator(final int firstRecord) throws IOException {
//        seek(firstRecord);
//
//    }

    public CompletableFuture<Void> read(final int firstRecord, final int maxRecords, final Consumer<CsvRow> consumer) {
        return getOffset(firstRecord).thenAccept(offset -> {
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
        });
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
            .thenApply(unused -> positions.get(record - 1));
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

    public CompletableFuture<Integer> size() {
        return scanner.thenApply(unused -> positions.size() + 1);
    }

    @Override
    public void close() throws IOException {
        scanner.cancel(true);
        raf.close();
    }

    public static RandomAccessCsvReaderBuilder builder() {
        return new RandomAccessCsvReaderBuilder();
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
            this.commentCharacter = commentCharacter;
            return this;
        }

        public RandomAccessCsvReader build(final Path file) throws IOException {
            return build(file, StandardCharsets.UTF_8);
        }

        public RandomAccessCsvReader build(final Path file, final Charset charset) throws IOException {
            return new RandomAccessCsvReader(file, charset, quoteCharacter, fieldSeparator, commentStrategy,
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
