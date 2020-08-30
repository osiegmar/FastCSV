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
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class NamedCsvReader implements Iterable<NamedCsvRow>, Closeable {

    private final CsvReader csvReader;
    private final Iterator<CsvRow> csvIterator;
    private final Iterator<NamedCsvRow> namedCsvIterator;

    private List<String> header;
    private Map<String, Integer> headerMap;
    private boolean isInitialized;

    public NamedCsvReader(final CsvReader csvReader) {
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new CsvRowIterator();
    }

    public List<String> getHeader() {
        if (!isInitialized) {
            initialize();
        }
        return header;
    }

    private void initialize() {
        if (!csvIterator.hasNext()) {
            header = Collections.emptyList();
            headerMap = Collections.emptyMap();
        } else {
            final CsvRow firstRow = csvIterator.next();

            header = Collections.unmodifiableList(firstRow.getFields());
            final Map<String, Integer> map = new LinkedHashMap<>(header.size());

            int i = 0;
            for (Iterator<String> iterator = header.iterator(); iterator.hasNext(); i++) {
                final String field = iterator.next();
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

    @Override
    public Spliterator<NamedCsvRow> spliterator() {
        return new CsvRowSpliterator(iterator());
    }

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

    private static final class CsvRowSpliterator implements Spliterator<NamedCsvRow> {

        private static final int CHARACTERISTICS = ORDERED | DISTINCT | NONNULL | IMMUTABLE;

        private final Iterator<NamedCsvRow> iterator;

        CsvRowSpliterator(final Iterator<NamedCsvRow> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super NamedCsvRow> action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.next());
            return true;
        }

        @Override
        public void forEachRemaining(final Consumer<? super NamedCsvRow> action) {
            iterator.forEachRemaining(action);
        }

        @Override
        public Spliterator<NamedCsvRow> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return CHARACTERISTICS;
        }

    }

}
