/*
 * Copyright 2020 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
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
    private final Iterator<CsvRow> csvIterator;
    private final Iterator<NamedCsvRow> namedCsvIterator;

    private String[] header;
    private Map<String, Integer> headerMap;
    private boolean isInitialized;

    NamedCsvReader(final CsvReader csvReader) {
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new CsvRowIterator();
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
    public Iterator<NamedCsvRow> iterator() {
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
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }

    private final class CsvRowIterator implements Iterator<NamedCsvRow> {

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public NamedCsvRow next() {
            return new NamedCsvRowImpl(csvIterator.next(), headerMap);
        }

    }

}
