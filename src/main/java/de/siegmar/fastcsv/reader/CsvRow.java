/*
 * Copyright 2015 Oliver Siegmar
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a single CSV row.
 *
 * @author Oliver Siegmar
 */
public final class CsvRow {

    /**
     * The original line number (empty lines may be skipped).
     */
    private final long originalLineNumber;

    private final Map<String, Integer> headerMap;
    private final List<String> fields;

    CsvRow(final long originalLineNumber, final Map<String, Integer> headerMap,
           final List<String> fields) {

        this.originalLineNumber = originalLineNumber;
        this.headerMap = headerMap;
        this.fields = fields;
    }

    /**
     * Returns the original line number (starting with 1). On multi-line rows this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReaderBuilder#skipEmptyRows(boolean)}.
     *
     * @return the original line number
     */
    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    /**
     * Gets a field value by its index (starting with 0).
     *
     * @param index index of the field to return
     * @return field value
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String getField(final int index) {
        return fields.get(index);
    }

    /**
     * Gets a field value by its name.
     *
     * @param name field name
     * @return field value, {@code null} if this row has no such field
     * @throws IllegalStateException if CSV is read without headers -
     *                               see {@link CsvReaderBuilder#containsHeader(boolean)}
     */
    public String getField(final String name) {
        if (headerMap == null) {
            throw new IllegalStateException("No header available");
        }

        final Integer col = headerMap.get(name);
        if (col != null && col < fields.size()) {
            return fields.get(col);
        }

        return null;
    }

    /**
     * Gets all fields of this row as an unmodifiable List.
     *
     * @return all fields of this row
     */
    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Gets an unmodifiable map of header names and field values of this row.
     * <p>
     * The map will always contain all header names - even if their value is {@code null}.
     *
     * @return an unmodifiable map of header names and field values of this row
     * @throws IllegalStateException if CSV is read without headers - see
     *                               {@link CsvReaderBuilder#containsHeader(boolean)}
     */
    public Map<String, String> getFieldMap() {
        if (headerMap == null) {
            throw new IllegalStateException("No header available");
        }

        final Map<String, String> fieldMap = new LinkedHashMap<>(headerMap.size());
        for (final Map.Entry<String, Integer> header : headerMap.entrySet()) {
            final String key = header.getKey();
            final Integer col = headerMap.get(key);
            final String val = col != null && col < fields.size() ? fields.get(col) : null;
            fieldMap.put(key, val);
        }

        return Collections.unmodifiableMap(fieldMap);
    }

    /**
     * Gets the number of fields of this row.
     *
     * @return the number of fields of this row
     */
    public int getFieldCount() {
        return fields.size();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CsvRow{");
        sb.append("originalLineNumber=");
        sb.append(originalLineNumber);
        sb.append(", ");

        sb.append("fields=");
        if (headerMap != null) {
            sb.append('{');
            for (final Iterator<Map.Entry<String, String>> it =
                 getFieldMap().entrySet().iterator(); it.hasNext();) {

                final Map.Entry<String, String> entry = it.next();
                sb.append(entry.getKey());
                sb.append('=');
                if (entry.getValue() != null) {
                    sb.append(entry.getValue());
                }
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append('}');
        } else {
            sb.append(fields.toString());
        }

        sb.append('}');
        return sb.toString();
    }

}
