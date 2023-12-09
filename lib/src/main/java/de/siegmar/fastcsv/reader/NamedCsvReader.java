package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Header name based CSV reader implementation.
 * <p>
 * Example use:
 * {@snippet :
 * try (NamedCsvReader csv = NamedCsvReader.from(CsvReader.builder().build(file))) {
 *     for (NamedCsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 */
public final class NamedCsvReader implements Iterable<NamedCsvRecord>, Closeable {

    private final CsvReader csvReader;
    private final CloseableIterator<CsvRecord> csvIterator;
    private final CloseableIterator<NamedCsvRecord> namedCsvIterator;

    private String[] header;

    private NamedCsvReader(final CsvReader csvReader, final List<String> header) {
        if (csvReader.getCommentStrategy() == CommentStrategy.READ) {
            throw new IllegalStateException("Can't read from a CsvReader with commentStrategy set to READ");
        }
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new NamedCsvRecordIterator(csvIterator);
        if (header != null) {
            this.header = header.toArray(new String[0]);
        }
    }

    /**
     * Builds a name based CSV reader based on a regular CsvReader.
     *
     * @param csvReader the reader this name based reader should work on.
     * @return a new instance of this class.
     */
    public static NamedCsvReader from(final CsvReader csvReader) {
        Objects.requireNonNull(csvReader, "csvReader must not be null");
        return new NamedCsvReader(csvReader, null);
    }

    /**
     * Builds a name based CSV reader based on a regular CsvReader.
     *
     * @param csvReader the reader this name based reader should work on.
     * @param header    a custom header name list. May contain duplicates.
     * @return a new instance of this class.
     * @throws NullPointerException if csvReader, header or a header element is {@code null}
     */
    public static NamedCsvReader from(final CsvReader csvReader, final List<String> header) {
        Objects.requireNonNull(csvReader, "csvReader must not be null");
        Objects.requireNonNull(header, "header must not be null");
        return new NamedCsvReader(csvReader, header);
    }

    /**
     * Returns the header fields. Can be called at any time.
     *
     * @return the header fields
     */
    public List<String> getHeader() {
        if (header == null) {
            header = initialize();
        }
        return Collections.unmodifiableList(Arrays.asList(header));
    }

    private String[] initialize() {
        return csvIterator.hasNext()
            ? csvIterator.next().fields
            : new String[] {};
    }

    /**
     * Returns an iterator over elements of type {@link NamedCsvRecord}.
     * <p>
     * The returned iterator is not thread-safe.
     * Don't forget to close the returned iterator when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent.
     *
     * @return an iterator over the named CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException if any other problem occurs when parsing the CSV data.
     * @see #stream()
     */
    @Override
    public CloseableIterator<NamedCsvRecord> iterator() {
        if (header == null) {
            header = initialize();
        }
        return namedCsvIterator;
    }

    /**
     * Returns a {@link Spliterator} over elements of type {@link NamedCsvRecord}.
     * <p>
     * The returned spliterator is not thread-safe.
     * Don't forget to invoke {@link #close()} when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent.
     *
     * @return a spliterator over the named CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException if any other problem occurs when parsing the CSV data.
     * @see #stream()
     */
    @Override
    public Spliterator<NamedCsvRecord> spliterator() {
        return new CsvRecordSpliterator<>(iterator());
    }

    /**
     * Returns a sequential {@link Stream} with this reader as its source.
     * <p>
     * The returned stream is not thread-safe.
     * Don't forget to close the returned stream when you're done.
     * <br>
     * This method is idempotent.
     *
     * @return a sequential stream of named CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException if any other problem occurs when parsing the CSV data.
     * @see #iterator()
     */
    public Stream<NamedCsvRecord> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(() -> {
            try {
                close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvReader.class.getSimpleName() + "[", "]")
            .add("header=" + Arrays.toString(header))
            .add("csvReader=" + csvReader)
            .toString();
    }

    private class NamedCsvRecordIterator implements CloseableIterator<NamedCsvRecord> {

        private final CloseableIterator<CsvRecord> csvIterator;

        NamedCsvRecordIterator(final CloseableIterator<CsvRecord> csvIterator) {
            this.csvIterator = csvIterator;
        }

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public NamedCsvRecord next() {
            return new NamedCsvRecord(header, csvIterator.next());
        }

        @Override
        public void close() throws IOException {
            csvIterator.close();
        }

    }

}
