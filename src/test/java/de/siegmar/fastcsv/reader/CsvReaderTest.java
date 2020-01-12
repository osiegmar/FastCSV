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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class CsvReaderTest {

    private CsvReader csvReader;

    @BeforeMethod
    public void init() {
        csvReader = new CsvReader();
    }

    // null / empty input

    @Test(expectedExceptions = NullPointerException.class)
    public void nullInput() throws IOException {
        parse(findBugsSafeNullInput());
    }

    private static String findBugsSafeNullInput() {
        return null;
    }

    public void empty() throws IOException {
        assertNull(parse("").nextRow());
    }

    public void simple() throws IOException {
        assertEquals(readCsvRow("foo").getField(0), "foo");
    }

    public void emptyContainer() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("");
        assertNotNull(csv);
        assertNull(csv.getHeader());
        assertEquals(csv.getRowCount(), 0);
        assertEquals(csv.getRows(), Collections.<CsvRow>emptyList());
    }

    // skipped rows

    public void singleRowNoSkipEmpty() throws IOException {
        csvReader.setSkipEmptyRows(false);
        assertNull(parse("").nextRow());
    }

    public void multipleRowsNoSkipEmpty() throws IOException {
        csvReader.setSkipEmptyRows(false);
        final CsvContainer csv = read("\n\n");

        final List<CsvRow> rows = csv.getRows();
        assertEquals(rows.size(), 2);

        int line = 1;
        for (final CsvRow row : rows) {
            assertEquals(row.getFieldCount(), 1);
            assertEquals(row.getFields(), Collections.singletonList(""));
            assertEquals(row.getOriginalLineNumber(), line++);
        }
    }

    public void skippedRows() throws IOException {
        final CsvContainer csv = read("\n\nfoo\n\nbar\n\n");
        assertEquals(csv.getRowCount(), 2);

        final CsvRow row1 = csv.getRow(0);
        assertEquals(row1.getOriginalLineNumber(), 3);
        assertEquals(row1.getField(0), "foo");

        final CsvRow row2 = csv.getRow(1);
        assertEquals(row2.getOriginalLineNumber(), 5);
        assertEquals(row2.getField(0), "bar");
    }

    // different field count

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

    @Test(expectedExceptions = IOException.class)
    public void differentFieldCountFail() throws IOException {
        csvReader.setErrorOnDifferentFieldCount(true);
        csvReader.setSkipEmptyRows(false);

        read("foo\nbar,baz");
    }

    // field by index

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void getNonExistingFieldByIndex() throws IOException {
        final String findbugsSafeReturn = parse("foo").nextRow().getField(1);
        fail("must not return: " + findbugsSafeReturn);
    }

    // field by name (header)

    public void getFieldByName() throws IOException {
        csvReader.setContainsHeader(true);
        assertEquals(parse("foo\nbar").nextRow().getField("foo"), "bar");
    }

    public void getHeader() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("foo,bar\n1,2");
        assertEquals(csv.getHeader(), Arrays.asList("foo", "bar"));
    }

    public void getHeaderEmptyRows() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvContainer csv = read("foo,bar");
        assertEquals(csv.getHeader(), Arrays.asList("foo", "bar"));
        assertEquals(csv.getRowCount(), 0);
        assertEquals(csv.getRows(), Collections.<CsvRow>emptyList());
    }

    // Request field by name, but headers are not enabled
    @Test(expectedExceptions = IllegalStateException.class)
    public void getFieldByNameWithoutHeader() throws IOException {
        parse("foo\n").nextRow().getField("bar");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getNonExistingHeader() throws IOException {
        final CsvParser csv = parse("foo\n");
        csv.nextRow();
        csv.getHeader();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getNonExistingFieldMap() throws IOException {
        final CsvParser csv = parse("foo\n");
        final CsvRow csvRow = csv.nextRow();
        csvRow.getFieldMap();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void getHeaderWithoutNextRowCall() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvParser csv = parse("foo\n");
        csv.getHeader();
    }

    // Request field by name, but column name doesn't exist
    public void getNonExistingFieldByName() throws IOException {
        csvReader.setContainsHeader(true);
        assertNull(parse("foo\nfaz").nextRow().getField("bar"));
    }

    // enclosure escaping

    public void escapedQuote() throws IOException {
        assertEquals(readCsvRow("foo,\"bar \"\"is\"\" ok\"").getField(1), "bar \"is\" ok");
    }

    public void handlesEmptyQuotedFieldsAtEndOfRow() throws IOException {
        assertEquals(readCsvRow("foo,\"\"").getField(1), "");
    }

    public void dataAfterNewlineAfterEnclosure() throws IOException {
        CsvContainer csv = read("\"foo\"\nbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");

        csv = read("\"foo\"\rbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");

        csv = read("\"foo\"\r\nbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");
    }

    public void invalidQuotes() throws IOException {
        assertEquals(readRow("bbb\"a\", ccc,ddd\"a,b\"eee,fff,ggg\"a\"\"b,\",a, b"),
            Arrays.asList(
                "bbb\"a\"",
                " ccc",
                "ddd\"a",
                "b\"eee",
                "fff",
                "ggg\"a\"\"b",
                ",a, b"
            ));
    }

    public void textBeforeQuotes() throws IOException {
        assertEquals(readRow("a\"b\",c"), Arrays.asList("a\"b\"", "c"));
    }

    public void textAfterQuotes() throws IOException {
        assertEquals(readRow("\"a\"b,c"), Arrays.asList("ab", "c"));
    }

    public void spaceBeforeQuotes() throws IOException {
        assertEquals(readRow(" \"a\",b"), Arrays.asList(" \"a\"", "b"));
    }

    public void spaceAfterQuotes() throws IOException {
        assertEquals(readRow("\"a\" ,b"), Arrays.asList("a ", "b"));
    }

    public void openingQuotes() throws IOException {
        assertEquals(readCsvRow("\"aaa").getField(0), "aaa");
    }

    public void closingQuotes() throws IOException {
        assertEquals(readCsvRow("aaa\"").getField(0), "aaa\"");
    }

    // line breaks

    public void lineFeed() throws IOException {
        final CsvContainer csv = read("foo\nbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");
    }

    public void carriageReturn() throws IOException {
        final CsvContainer csv = read("foo\rbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");
    }

    public void carriageReturnLineFeed() throws IOException {
        final CsvContainer csv = read("foo\r\nbar");
        assertEquals(csv.getRowCount(), 2);
        assertEquals(csv.getRow(0).getField(0), "foo");
        assertEquals(csv.getRow(1).getField(0), "bar");
    }

    // line numbering

    public void lineNumbering() throws IOException {
        final CsvParser csv = parse("\"a multi-\nline string\"\n\"another\none\"");

        CsvRow row = csv.nextRow();
        assertEquals(row.getFields(), Collections.singletonList("a multi-\nline string"));
        assertEquals(row.getOriginalLineNumber(), 1);

        row = csv.nextRow();
        assertEquals(row.getFields(), Collections.singletonList("another\none"));
        assertEquals(row.getOriginalLineNumber(), 3);
    }

    // to string

    public void toStringWithoutHeader() throws IOException {
        final CsvRow csvRow = parse("fieldA,fieldB\n").nextRow();
        assertEquals(csvRow.toString(), "CsvRow{originalLineNumber=1, fields=[fieldA, fieldB]}");
    }

    public void toStringWithHeader() throws IOException {
        csvReader.setContainsHeader(true);
        final CsvRow csvRow = parse("headerA,headerB,headerC\nfieldA,fieldB\n").nextRow();
        assertEquals(csvRow.toString(),
            "CsvRow{originalLineNumber=2, fields={headerA=fieldA, headerB=fieldB, headerC=}}");
    }

    // test helpers

    private CsvRow readCsvRow(final String data) throws IOException {
        try (final CsvParser csvParser = parse(data)) {
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
