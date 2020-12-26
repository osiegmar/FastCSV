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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main class for reading CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvReader implements Closeable {

    private final RowReader rowReader;
    private final boolean containsHeader;
    private final boolean skipEmptyRows;
    private final boolean errorOnDifferentFieldCount;

    private Map<String, Integer> headerMap;
    private List<String> headerList;
    private long lineNo;
    private int firstLineFieldCount = -1;

    CsvReader(final Reader reader, final char fieldSeparator, final char textDelimiter,
              final boolean containsHeader, final boolean skipEmptyRows,
              final boolean errorOnDifferentFieldCount) {

        rowReader = new RowReader(reader, fieldSeparator, textDelimiter);
        this.containsHeader = containsHeader;
        this.skipEmptyRows = skipEmptyRows;
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
    }

    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    /**
     * Returns the header fields - {@code null} if no header exists. The returned list is
     * unmodifiable. Use {@link CsvReaderBuilder#containsHeader(boolean)} to enable header parsing.
     * Also note, that the header is only available <strong>after</strong> first invocation of
     * {@link #nextRow()}.
     *
     * @return the header fields
     * @throws IllegalStateException if header parsing is not enabled or {@link #nextRow()} wasn't
     * called before.
     */
    public List<String> getHeader() {
        if (!containsHeader) {
            throw new IllegalStateException("No header available - header parsing is disabled");
        }
        if (lineNo == 0) {
            throw new IllegalStateException("No header available - call nextRow() first");
        }
        return headerList;
    }

    /**
     * Reads a complete {@link CsvRow} that might be made up of multiple lines in the underlying
     * CSV file.
     *
     * @return a CsvRow or {@code null} if end of file reached
     * @throws IOException if an error occurred while reading data
     */
    public CsvRow nextRow() throws IOException {
        while (!rowReader.isFinished()) {
            final long startingLineNo = lineNo + 1;
            final RowReader.Line line = rowReader.readLine();
            final String[] currentFields = line.getFields();
            lineNo += line.getLines();

            final int fieldCount = currentFields.length;

            // reached end of data in a new line?
            if (fieldCount == 0) {
                break;
            }

            // skip empty rows
            if (skipEmptyRows && fieldCount == 1 && currentFields[0].isEmpty()) {
                continue;
            }

            // check the field count consistency on every row
            if (errorOnDifferentFieldCount) {
                if (firstLineFieldCount == -1) {
                    firstLineFieldCount = fieldCount;
                } else if (fieldCount != firstLineFieldCount) {
                    throw new IOException(
                        String.format("Line %d has %d fields, but first line has %d fields",
                            lineNo, fieldCount, firstLineFieldCount));
                }
            }

            final List<String> fieldList = Arrays.asList(currentFields);

            // initialize header
            if (containsHeader && headerList == null) {
                initHeader(fieldList);
                continue;
            }

            return new CsvRow(startingLineNo, headerMap, fieldList);
        }

        return null;
    }

    private void initHeader(final List<String> currentFields) {
        headerList = Collections.unmodifiableList(currentFields);

        final Map<String, Integer> localHeaderMap = new LinkedHashMap<>(currentFields.size());
        for (int i = 0; i < currentFields.size(); i++) {
            final String field = currentFields.get(i);
            if (field != null && !field.isEmpty() && !localHeaderMap.containsKey(field)) {
                localHeaderMap.put(field, i);
            }
        }
        headerMap = Collections.unmodifiableMap(localHeaderMap);
    }

    @Override
    public void close() throws IOException {
        rowReader.close();
    }

}
