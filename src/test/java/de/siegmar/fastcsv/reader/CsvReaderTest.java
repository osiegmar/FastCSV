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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CsvReaderTest {

    private CsvReader csvReader = new CsvReader();

    // null / empty input

    @Test
    public void nullInput() {
        assertThrows(NullPointerException.class, () -> parse(findBugsSafeNullInput()));
    }

    private static String findBugsSafeNullInput() {
        return null;
    }

    @Test
    public void empty() throws IOException {
        assertNull(parse("").nextRow());
    }

    @Test
    public void simple() throws IOException {
        assertEquals("foo", readCsvRow("foo").getField(0));
    }

    @Test
    public void emptyContainer() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("");
        assertNotNull(csv);
        assertNull(csv.getHeader());
        assertEquals(0, csv.getRowCount());
        assertEquals(Collections.<CsvRow>emptyList(), csv.getRows());
    }

    // skipped rows

    @Test
    public void singleRowNoSkipEmpty() throws IOException {
        csvReader.setSkipEmptyRows(false);
        assertNull(parse("").nextRow());
    }

    @Test
    public void multipleRowsNoSkipEmpty() throws IOException {
        csvReader.setSkipEmptyRows(false);
        final CsvContainer csv = read("\n\n");

        final List<CsvRow> rows = csv.getRows();
        assertEquals(2, rows.size());

        int line = 1;
        for (final CsvRow row : rows) {
            assertEquals(1, row.getFieldCount());
            assertEquals(Collections.singletonList(""), row.getFields());
            assertEquals(line++, row.getOriginalLineNumber());
        }
    }

    @Test
    public void skippedRows() throws IOException {
        final CsvContainer csv = read("\n\nfoo\n\nbar\n\n");
        assertEquals(2, csv.getRowCount());

        final CsvRow row1 = csv.getRow(0);
        assertEquals(3, row1.getOriginalLineNumber());
        assertEquals("foo", row1.getField(0));

        final CsvRow row2 = csv.getRow(1);
        assertEquals(5, row2.getOriginalLineNumber());
        assertEquals("bar", row2.getField(0));
    }

    // different field count

    @Test
    public void differentFieldCountSuccess() throws IOException {
        csvReader.setErrorOnDifferentFieldCount(true);
        csvReader.setSkipEmptyRows(false);

        read("foo\nbar");
        read("foo\nbar\n");

        read("foo,bar\nfaz,baz");
        read("foo,bar\nfaz,baz\n");

        read("foo,bar\n,baz");
        read(",bar\nfaz,baz");
    }

    @Test
    public void differentFieldCountFail() {
        csvReader.setErrorOnDifferentFieldCount(true);
        csvReader.setSkipEmptyRows(false);

        assertThrows(IOException.class, () -> read("foo\nbar,baz"));
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    public void getNonExistingFieldByIndex() throws IOException {
        final CsvRow csvRow = parse("foo").nextRow();
        assertThrows(IndexOutOfBoundsException.class, () -> csvRow.getField(1).toString());
    }

    // field by name (header)

    @Test
    public void getFieldByName() throws IOException {
        csvReader.setContainsHeader(true);
        assertEquals("bar", parse("foo\nbar").nextRow().getField("foo"));
    }

    @Test
    public void getHeader() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("foo,bar\n1,2");
        assertEquals(Arrays.asList("foo", "bar"), csv.getHeader());
    }

    @Test
    public void getHeaderEmptyRows() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("foo,bar");
        assertEquals(Arrays.asList("foo", "bar"), csv.getHeader());
        assertEquals(0, csv.getRowCount());
        assertEquals(Collections.<CsvRow>emptyList(), csv.getRows());
    }

    // Request field by name, but headers are not enabled
    @Test
    public void getFieldByNameWithoutHeader() throws IOException {
        final CsvRow csvRow = parse("foo\n").nextRow();
        assertThrows(IllegalStateException.class, () -> csvRow.getField("bar"));
    }

    @Test
    public void getNonExistingHeader() throws IOException {
        final CsvParser csv = parse("foo\n");
        csv.nextRow();
        assertThrows(IllegalStateException.class, csv::getHeader);
    }

    @Test
    public void getNonExistingFieldMap() throws IOException {
        final CsvParser csv = parse("foo\n");
        final CsvRow csvRow = csv.nextRow();
        assertThrows(IllegalStateException.class, csvRow::getFieldMap);
    }

    @Test
    public void getHeaderWithoutNextRowCall() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvParser csv = parse("foo\n");
        assertThrows(IllegalStateException.class, csv::getHeader);
    }

    // Request field by name, but column name doesn't exist
    @Test
    public void getNonExistingFieldByName() throws IOException {
        csvReader.setContainsHeader(true);
        assertNull(parse("foo\nfaz").nextRow().getField("bar"));
    }

    // enclosure escaping

    @Test
    public void escapedQuote() throws IOException {
        assertEquals("bar \"is\" ok", readCsvRow("foo,\"bar \"\"is\"\" ok\"").getField(1));
    }

    @Test
    public void handlesEmptyQuotedFieldsAtEndOfRow() throws IOException {
        assertEquals("", readCsvRow("foo,\"\"").getField(1));
    }

    @Test
    public void dataAfterNewlineAfterEnclosure() throws IOException {
        CsvContainer csv = read("\"foo\"\nbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));

        csv = read("\"foo\"\rbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));

        csv = read("\"foo\"\r\nbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));
    }

    @Test
    public void invalidQuotes() throws IOException {
        assertEquals(Arrays.asList(
            "bbb\"a\"",
            " ccc",
            "ddd\"a",
            "b\"eee",
            "fff",
            "ggg\"a\"\"b",
            ",a, b"
            ),
            readRow("bbb\"a\", ccc,ddd\"a,b\"eee,fff,ggg\"a\"\"b,\",a, b"));
    }

    @Test
    public void textBeforeQuotes() throws IOException {
        assertEquals(Arrays.asList("a\"b\"", "c"), readRow("a\"b\",c"));
    }

    @Test
    public void textAfterQuotes() throws IOException {
        assertEquals(Arrays.asList("ab", "c"), readRow("\"a\"b,c"));
    }

    @Test
    public void spaceBeforeQuotes() throws IOException {
        assertEquals(Arrays.asList(" \"a\"", "b"), readRow(" \"a\",b"));
    }

    @Test
    public void spaceAfterQuotes() throws IOException {
        assertEquals(Arrays.asList("a ", "b"), readRow("\"a\" ,b"));
    }

    @Test
    public void openingQuotes() throws IOException {
        assertEquals("aaa", readCsvRow("\"aaa").getField(0));
    }

    @Test
    public void closingQuotes() throws IOException {
        assertEquals("aaa\"", readCsvRow("aaa\"").getField(0));
    }

    // line breaks

    @Test
    public void lineFeed() throws IOException {
        final CsvContainer csv = read("foo\nbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));
    }

    @Test
    public void carriageReturn() throws IOException {
        final CsvContainer csv = read("foo\rbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));
    }

    @Test
    public void carriageReturnLineFeed() throws IOException {
        final CsvContainer csv = read("foo\r\nbar");
        assertEquals(2, csv.getRowCount());
        assertEquals("foo", csv.getRow(0).getField(0));
        assertEquals("bar", csv.getRow(1).getField(0));
    }

    // line numbering

    @Test
    public void lineNumbering() throws IOException {
        final CsvParser csv = parse("\"a multi-\nline string\"\n\"another\none\"");

        CsvRow row = csv.nextRow();
        assertEquals(Collections.singletonList("a multi-\nline string"), row.getFields());
        assertEquals(1, row.getOriginalLineNumber());

        row = csv.nextRow();
        assertEquals(Collections.singletonList("another\none"), row.getFields());
        assertEquals(3, row.getOriginalLineNumber());
    }

    // to string

    @Test
    public void toStringWithoutHeader() throws IOException {
        final CsvRow csvRow = parse("fieldA,fieldB\n").nextRow();
        assertEquals("CsvRow{originalLineNumber=1, fields=[fieldA, fieldB]}", csvRow.toString());
    }

    @Test
    public void toStringWithHeader() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvRow csvRow = parse("headerA,headerB,headerC\nfieldA,fieldB\n").nextRow();
        assertEquals(
            "CsvRow{originalLineNumber=2, fields={headerA=fieldA, headerB=fieldB, headerC=}}",
            csvRow.toString());
    }

    // test helpers

    private CsvRow readCsvRow(final String data) throws IOException {
        try (CsvParser csvParser = parse(data)) {
            final CsvRow csvRow = csvParser.nextRow();
            assertNull(csvParser.nextRow());
            return csvRow;
        }
    }

    private List<String> readRow(final String data) throws IOException {
        return readCsvRow(data).getFields();
    }

    private CsvContainer read(final String data) throws IOException {
        return csvReader.read(new StringReader(data));
    }

    private CsvParser parse(final String data) throws IOException {
        return csvReader.parse(new StringReader(data));
    }

}
