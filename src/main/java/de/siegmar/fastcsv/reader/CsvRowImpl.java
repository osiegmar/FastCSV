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
import java.util.List;
import java.util.StringJoiner;

/**
 * This class represents a single CSV row.
 */
final class CsvRowImpl implements CsvRow {

    /**
     * The original line number (empty lines may be skipped).
     */
    private final long originalLineNumber;

    private final List<String> fields;

    CsvRowImpl(final long originalLineNumber, final List<String> fields) {
        this.originalLineNumber = originalLineNumber;
        this.fields = fields;
    }

    @Override
    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    @Override
    public String getField(final int index) {
        return fields.get(index);
    }

    @Override
    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }

    @Override
    public int getFieldCount() {
        return fields.size();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRowImpl.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fields=" + fields)
            .toString();
    }

}
