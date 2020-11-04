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

import java.util.List;

/**
 * Interface for a index based CSV-row.
 */
public interface CsvRow {

    /**
     * Returns the original line number (starting with 1). On multi-line rows this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReaderBuilder#skipEmptyRows(boolean)}.
     *
     * @return the original line number
     */
    long getOriginalLineNumber();

    /**
     * Gets a field value by its index (starting with 0).
     *
     * @param index index of the field to return
     * @return field value
     * @throws IndexOutOfBoundsException if index is out of range
     */
    String getField(int index);

    /**
     * Gets all fields of this row as an unmodifiable List.
     *
     * @return all fields of this row
     */
    List<String> getFields();

    /**
     * Gets the number of fields of this row.
     *
     * @return the number of fields of this row
     */
    int getFieldCount();

}
