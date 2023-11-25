package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    private List<String> header;

    private NamedCsvReader(final CsvReader csvReader, final List<String> header) {
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new NamedCsvRecordIterator(csvIterator);
        if (header != null) {
            this.header = List.copyOf(header);
        }
    }

    /**
     * Builds a name based CSV reader based on a regular CsvReader.
     *
     * @param csvReader the reader this name based reader should work on.
     * @return a new instance of this class.
     */
    public static NamedCsvReader from(final CsvReader csvReader) {
        return new NamedCsvReader(Objects.requireNonNull(csvReader, "csvReader must not be null"), null);
    }

    /**
     * Builds a name based CSV reader based on a regular CsvReader.
     *
     * @param csvReader the reader this name based reader should work on.
     * @param header    a custom header name list.
     * @return a new instance of this class.
     */
    public static NamedCsvReader from(final CsvReader csvReader, final List<String> header) {
        return new NamedCsvReader(
            Objects.requireNonNull(csvReader, "csvReader must not be null"),
            Objects.requireNonNull(header, "header must not be null")
        );
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
        return header;
    }

    private List<String> initialize() {
        return csvIterator.hasNext()
            ? csvIterator.next().getFields()
            : Collections.emptyList();
    }

    @Override
    public CloseableIterator<NamedCsvRecord> iterator() {
        if (header == null) {
            header = initialize();
        }
        return namedCsvIterator;
    }

    @Override
    public Spliterator<NamedCsvRecord> spliterator() {
        return new CsvRecordSpliterator<>(iterator());
    }

    /**
     * Creates a new sequential {@link Stream} from this instance.
     * <p>
     * A close handler is registered by this method in order to close the underlying resources.
     * Don't forget to close the returned stream when you're done.
     *
     * @return a new sequential {@link Stream}.
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
            .add("header=" + header)
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
