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
    private final CloseableIterator<CsvRow> csvIterator;
    private final CloseableIterator<NamedCsvRow> namedCsvIterator;

    private String[] header;
    private Map<String, Integer> headerMap;
    private boolean isInitialized;

    NamedCsvReader(final CsvReader csvReader) {
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new NamedCsvRowIterator();
    }

    /**
     * Returns the header columns. Can be called at any time. Reads data from data source if not
     * done yet.
     *
     * @return the header columns
     */
    public String[] getHeader() {
        if (!isInitialized) {
            initialize();
        }
        return header.clone();
    }

    private void initialize() {
        if (!csvIterator.hasNext()) {
            header = new String[0];
            headerMap = Collections.emptyMap();
        } else {
            final CsvRow firstRow = csvIterator.next();

            header = firstRow.getFields();
            final Map<String, Integer> map = new LinkedHashMap<>(header.length);

            for (int i = 0; i < header.length; i++) {
                final String field = header[i];
                final Integer put = map.put(field, i);
                if (put != null) {
                    throw new IllegalStateException("Duplicate header field '" + field + "' found");
                }
            }

            headerMap = Collections.unmodifiableMap(map);
        }

        isInitialized = true;
    }

    @Override
    public CloseableIterator<NamedCsvRow> iterator() {
        if (!isInitialized) {
            initialize();
        }

        return namedCsvIterator;
    }

    /**
     * Constructs and returns a new {@link CsvRowSpliterator}.
     *
     * @return a new {@link CsvRowSpliterator} instance
     */
    @Override
    public Spliterator<NamedCsvRow> spliterator() {
        return new CsvRowSpliterator<>(iterator());
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

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public NamedCsvRow next() {
            return new NamedCsvRowImpl(csvIterator.next(), headerMap);
        }

        @Override
        public void close() throws IOException {
            csvIterator.close();
        }

    }

}
