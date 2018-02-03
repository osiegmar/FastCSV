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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for parsing CSV data from the passed-in Reader.
 *
 * @author Oliver Siegmar
 */
public final class CsvParser implements Closeable {

    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_ROW_CAPACITY = 10;

    private static final int FIELD_MODE_RESET = 0;
    private static final int FIELD_MODE_QUOTED = 1;
    private static final int FIELD_MODE_NON_QUOTED = 2;
    private static final int FIELD_MODE_QUOTE_ON = 4;

    private final Reader reader;
    private final char fieldSeparator;
    private final char textDelimiter;
    private final boolean containsHeader;
    private final boolean skipEmptyRows;
    private final boolean errorOnDifferentFieldCount;
    private final char[] buf = new char[BUFFER_SIZE];
    private final ReusableStringBuilder currentField = new ReusableStringBuilder(512);

    private int bufPos;
    private int bufLen;
    private int prevChar = -1;
    private int copyStart;
    private Map<String, Integer> headerMap;
    private List<String> headerList;
    private long lineNo;
    private int firstLineFieldCount = -1;
    private int maxFieldCount;
    private boolean finished;

    CsvParser(final Reader reader, final char fieldSeparator, final char textDelimiter,
              final boolean containsHeader, final boolean skipEmptyRows,
              final boolean errorOnDifferentFieldCount) {
        this.reader = reader;
        this.fieldSeparator = fieldSeparator;
        this.textDelimiter = textDelimiter;
        this.containsHeader = containsHeader;
        this.skipEmptyRows = skipEmptyRows;
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
    }

    /**
     * Returns the header fields - {@code null} if no header exists. The returned list is
     * unmodifiable. Use {@link CsvReader#setContainsHeader(boolean)} to enable header parsing.
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
        while (!finished) {
            final long startingLineNo = ++lineNo;
            final List<String> currentFields = readLine();

            final int fieldCount = currentFields.size();

            // reached end of data in a new line?
            if (fieldCount == 0) {
                break;
            }

            // skip empty rows
            if (skipEmptyRows && fieldCount == 1 && currentFields.get(0).isEmpty()) {
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

            // remember maximum field count for array initialization in next loop iteration
            if (fieldCount > maxFieldCount) {
                maxFieldCount = fieldCount;
            }

            // initialize header
            if (containsHeader && headerList == null) {
                initHeader(currentFields);
                continue;
            }

            return new CsvRow(startingLineNo, headerMap, currentFields);
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

    /*
     * ugly, performance optimized code begins
     */
    private List<String> readLine() throws IOException {
        final List<String> currentFields =
            new ArrayList<>(maxFieldCount > 0 ? maxFieldCount : DEFAULT_ROW_CAPACITY);

        // get fields local for higher performance
        final ReusableStringBuilder localCurrentField = currentField;
        final char[] localBuf = buf;
        int localBufPos = bufPos;
        int localPrevChar = prevChar;
        int localCopyStart = copyStart;
        int copyLen = 0;

        int fieldMode = FIELD_MODE_RESET;

        while (true) {
            if (bufLen == localBufPos) {
                // end of buffer

                if (copyLen > 0) {
                    localCurrentField.append(localBuf, localCopyStart, copyLen);
                }
                bufLen = reader.read(localBuf, 0, localBuf.length);

                if (bufLen < 0) {
                    // end of data
                    finished = true;

                    if (localPrevChar == fieldSeparator || localCurrentField.hasContent()) {
                        currentFields.add(localCurrentField.toStringAndReset());
                    }

                    break;
                }

                localCopyStart = localBufPos = copyLen = 0;
            }

            final char c = localBuf[localBufPos++];

            if ((fieldMode & FIELD_MODE_QUOTE_ON) != 0) {
                if (c == textDelimiter) {
                    // End of quoted text
                    fieldMode &= ~(FIELD_MODE_QUOTE_ON);
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                        copyLen = 0;
                    }
                    localCopyStart = localBufPos;
                } else {
                    if (c == CR || c == LF && prevChar != CR) {
                        lineNo++;
                    }
                    copyLen++;
                }
            } else {
                if (c == fieldSeparator) {
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                        copyLen = 0;
                    }
                    currentFields.add(localCurrentField.toStringAndReset());
                    localCopyStart = localBufPos;
                    fieldMode = FIELD_MODE_RESET;
                } else if (c == textDelimiter && (fieldMode & FIELD_MODE_NON_QUOTED) == 0) {
                    // Quoted text starts
                    fieldMode = FIELD_MODE_QUOTED | FIELD_MODE_QUOTE_ON;

                    if (localPrevChar == textDelimiter) {
                        // escaped quote
                        copyLen++;
                    } else {
                        localCopyStart = localBufPos;
                    }
                } else if (c == CR) {
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                    }
                    currentFields.add(localCurrentField.toStringAndReset());
                    localPrevChar = c;
                    localCopyStart = localBufPos;
                    break;
                } else if (c == LF) {
                    if (localPrevChar != CR) {
                        if (copyLen > 0) {
                            localCurrentField.append(localBuf, localCopyStart, copyLen);
                        }
                        currentFields.add(localCurrentField.toStringAndReset());
                        localPrevChar = c;
                        localCopyStart = localBufPos;
                        break;
                    }
                    localCopyStart = localBufPos;
                } else {
                    copyLen++;
                    if (fieldMode == FIELD_MODE_RESET) {
                        fieldMode = FIELD_MODE_NON_QUOTED;
                    }
                }
            }

            localPrevChar = c;
        }

        // restore fields
        bufPos = localBufPos;
        prevChar = localPrevChar;
        copyStart = localCopyStart;

        return currentFields;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
