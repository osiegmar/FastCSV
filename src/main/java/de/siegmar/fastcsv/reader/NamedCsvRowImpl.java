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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringJoiner;

final class NamedCsvRowImpl implements NamedCsvRow {

    private final CsvRow row;
    private final Map<String, Integer> headerMap;

    NamedCsvRowImpl(final CsvRow row, final Map<String, Integer> headerMap) {
        this.row = row;
        this.headerMap = headerMap;
    }

    @Override
    public long getOriginalLineNumber() {
        return row.getOriginalLineNumber();
    }

    @Override
    public String getField(final int index) {
        return row.getField(index);
    }

    @Override
    public String getField(final String name) {
        return findField(name).orElseThrow(() ->
            new NoSuchElementException("No element with name '" + name + "' found. "
                + "Valid names are: " + headerMap.keySet()));
    }

    @Override
    public Optional<String> findField(final String name) {
        final Integer col = headerMap.get(name);
        return col != null && col < row.getFieldCount()
            ? Optional.of(row.getField(col))
            : Optional.empty();
    }

    @Override
    public List<String> getFields() {
        return row.getFields();
    }

    @Override
    public int getFieldCount() {
        return row.getFieldCount();
    }

    @Override
    public Map<String, String> getFieldMap() {
        final Map<String, String> fieldMap = new LinkedHashMap<>(headerMap.size());
        headerMap.forEach((name, idx) ->
            fieldMap.put(name, idx < row.getFieldCount() ? row.getField(idx) : null));

        return Collections.unmodifiableMap(fieldMap);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRowImpl.class.getSimpleName() + "[", "]")
            .add("headerMap=" + headerMap)
            .add("row=" + row)
            .toString();
    }

}
