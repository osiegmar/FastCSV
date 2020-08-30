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

/**
 * Class for holding a complete CSV file.
 *
 * @author Oliver Siegmar
 */
public final class NamedCsvContainer implements CsvContainer<NamedCsvRow> {

    private final List<String> header;
    private final List<NamedCsvRow> rows;

    NamedCsvContainer(final List<String> header, final List<NamedCsvRow> rows) {
        this.header = header;
        this.rows = rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public NamedCsvRow getRow(final int index) {
        return rows.get(index);
    }

    @Override
    public List<NamedCsvRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Returns the header row - might be {@code null} if no header exists.
     * The returned list is unmodifiable.
     *
     * @return the header row - might be {@code null} if no header exists
     */
    public List<String> getHeader() {
        return header;
    }

}
