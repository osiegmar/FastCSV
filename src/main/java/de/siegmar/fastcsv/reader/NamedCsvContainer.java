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

import java.util.List;

/**
 * Class for holding a complete CSV file.
 *
 * @author Oliver Siegmar
 */
public class NamedCsvContainer extends IndexedCsvContainer {

    private final List<String> header;

    NamedCsvContainer(final List<String> header, final List<CsvRow> rows) {
        super(rows);
        this.header = header;
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
