package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Header name based Csv reader implementation.
 *
 * Obtain via:
 * <pre>{@code
 * CsvReader.builder().build(...).withHeader()
 * }</pre>
 */
public final class NamedCsvReader implements Iterable<NamedCsvRow>, Closeable {

    private final CsvReader csvReader;
    private final CloseableIterator<NamedCsvRow> namedCsvIterator;

    private String[] header = new String[0];
    private Map<String, Integer> headerMap = Collections.emptyMap();
    private boolean headerInitialized;

    NamedCsvReader(final CsvReader csvReader) {
        this.csvReader = csvReader;
        namedCsvIterator = new NamedCsvRowIterator(csvReader.iterator());
    }

    /**
     * Returns the header columns. Can be called at any time. Reads data from data source if not
     * done yet.
     *
     * @return the header columns
     */
    public String[] getHeader() {
        while (namedCsvIterator.hasNext() && header.length == 0) {
            namedCsvIterator.next();
        }
        return header.clone();
    }

    private void initializeHeader(final CsvRow row) {
        header = row.getFields();
        final Map<String, Integer> map = new LinkedHashMap<>(header.length);

        for (int i = 0; i < header.length; i++) {
            final String field = header[i];
            final Integer put = map.put(field, i);
            if (put != null) {
                throw new IllegalStateException("Duplicate header field '" + field + "' found");
            }
        }

        headerMap = Collections.unmodifiableMap(map);
        headerInitialized = true;
    }

    @Override
    public CloseableIterator<NamedCsvRow> iterator() {
        return namedCsvIterator;
    }

    /**
     * Constructs and returns a new {@link CsvRowSpliterator}.
     *
     * @return a new {@link CsvRowSpliterator} instance
     */
    @Override
    public Spliterator<NamedCsvRow> spliterator() {
        return new CsvRowSpliterator<>(namedCsvIterator);
    }

    /**
     * Creates a new sequential {@code Stream} from this instance.
     *
     * @return a new sequential {@code Stream}.
     */
    public Stream<NamedCsvRow> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(() -> {
            try {
                close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }

    private class NamedCsvRowIterator implements CloseableIterator<NamedCsvRow> {

        private final CloseableIterator<CsvRow> csvIterator;

        NamedCsvRowIterator(final CloseableIterator<CsvRow> csvIterator) {
            this.csvIterator = csvIterator;
        }

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public NamedCsvRow next() {
            CsvRow row = csvIterator.next();

            if (row.isComment()) {
                return new NamedCsvRowImpl(row);
            }

            if (!headerInitialized) {
                initializeHeader(row);
                if (!csvIterator.hasNext()) {
                    return new NamedCsvRowImpl(row);
                }
                row = csvIterator.next();
            }

            return new NamedCsvRowImpl(row, headerMap);
        }

        @Override
        public void close() throws IOException {
            csvIterator.close();
        }

    }

}
