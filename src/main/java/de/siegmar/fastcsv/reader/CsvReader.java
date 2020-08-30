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
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is the main class for reading CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvReader implements Iterable<IndexedCsvRow>, Closeable {

    private final RowReader rowReader;
    private final boolean skipEmptyRows;
    private final boolean errorOnDifferentFieldCount;
    private final Iterator<IndexedCsvRow> csvRowIterator = new CsvRowIterator();

    private long lineNo;
    private int firstLineFieldCount = -1;

    CsvReader(final Reader reader, final char fieldSeparator, final char textDelimiter,
              final boolean skipEmptyRows, final boolean errorOnDifferentFieldCount) {

        rowReader = new RowReader(reader, fieldSeparator, textDelimiter);
        this.skipEmptyRows = skipEmptyRows;
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
    }

    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    public NamedCsvReader withHeader() {
        return new NamedCsvReader(this);
    }

    @Override
    public Iterator<IndexedCsvRow> iterator() {
        return csvRowIterator;
    }

    public Stream<IndexedCsvRow> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public void close() throws IOException {
        rowReader.close();
    }

    private final class CsvRowIterator implements Iterator<IndexedCsvRow> {

        private IndexedCsvRow nextRow;
        private boolean isEnd;

        @Override
        public boolean hasNext() {
            if (isEnd) {
                return false;
            }
            if (nextRow == null) {
                nextRow = fetch();

                if (nextRow == null) {
                    isEnd = true;
                    return false;
                }
            }

            return true;
        }

        @Override
        public IndexedCsvRow next() {
            if (isEnd) {
                throw new NoSuchElementException();
            }

            final IndexedCsvRow row;
            if (nextRow != null) {
                row = nextRow;
                nextRow = null;
            } else {
                row = fetch();
            }

            return row;
        }

        private IndexedCsvRow fetch() {
            try {
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

                    return new IndexedCsvRow(startingLineNo, Arrays.asList(currentFields));
                }

                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

}
