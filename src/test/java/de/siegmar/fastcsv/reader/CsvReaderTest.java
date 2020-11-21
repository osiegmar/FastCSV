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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Test;

public class CsvReaderTest {

    private final CsvReaderBuilder crb = CsvReader.builder();

    // null / empty input

    @Test
    public void nullInput() {
        assertThrows(NullPointerException.class, () -> parse(findBugsSafeNullInput()));
    }

    private static String findBugsSafeNullInput() {
        return null;
    }

    @Test
    public void empty() {
        final Iterator<CsvRow> it = parse("").iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void simple() {
        assertArrayEquals(asArray("foo"), readSingleRow("foo").getFields());
    }

    private static String[] asArray(final String... items) {
        return items;
    }

    @Test
    public void bom() {
        final byte[] bom8 = new byte[]{ (byte)0xEF, (byte)0xBB, (byte)0xBF, 'a', ',', 'b'};

        // Expecting trouble when reading BOM
        final Reader standardReader = new InputStreamReader(
            new ByteArrayInputStream(bom8), StandardCharsets.UTF_8);
        assertArrayEquals(asArray("\uFEFFa", "b"),
            crb.build(standardReader).iterator().next().getFields());

        // Reading BOM requires external support (e.g. org.apache.commons.io.input.BOMInputStream)
        final Reader bomReader = new InputStreamReader(new BOMInputStream(
            new ByteArrayInputStream(bom8)), StandardCharsets.UTF_8);
        assertArrayEquals(asArray("a", "b"),
            crb.build(bomReader).iterator().next().getFields());
    }

    // skipped rows

    @Test
    public void singleRowNoSkipEmpty() {
        crb.skipEmptyRows(false);
        assertFalse(parse("").iterator().hasNext());
    }

    @Test
    public void multipleRowsNoSkipEmpty() {
        crb.skipEmptyRows(false);
        final Iterator<CsvRow> it = parse("\n\n").iterator();

        CsvRow row = it.next();
        assertEquals(1, row.getFieldCount());
        assertEquals(1, row.getOriginalLineNumber());
        assertArrayEquals(asArray(""), row.getFields());

        row = it.next();
        assertEquals(1, row.getFieldCount());
        assertEquals(2, row.getOriginalLineNumber());
        assertArrayEquals(asArray(""), row.getFields());

        assertFalse(it.hasNext());
    }

    @Test
    public void skippedRows() {
        final List<CsvRow> csv = readAll("\n\nfoo\n\nbar\n\n");
        assertEquals(2, csv.size());

        final Iterator<CsvRow> it = csv.iterator();

        CsvRow row = it.next();
        assertEquals(3, row.getOriginalLineNumber());
        assertArrayEquals(asArray("foo"), row.getFields());

        row = it.next();
        assertEquals(5, row.getOriginalLineNumber());
        assertArrayEquals(asArray("bar"), row.getFields());
    }

    // different field count

    @Test
    public void differentFieldCountSuccess() {
        crb.errorOnDifferentFieldCount(true);

        readAll("foo\nbar");
        readAll("foo\nbar\n");

        readAll("foo,bar\nfaz,baz");
        readAll("foo,bar\nfaz,baz\n");

        readAll("foo,bar\n,baz");
        readAll(",bar\nfaz,baz");
    }

    @Test
    public void differentFieldCountFail() {
        crb.errorOnDifferentFieldCount(true);

        final UncheckedIOException e = assertThrows(UncheckedIOException.class,
            () -> readAll("foo\nbar,baz"));

        assertEquals("java.io.IOException: Line 2 has 2 fields, "
            + "but first line has 1 fields", e.getMessage());
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    public void getNonExistingFieldByIndex() {
        final CsvRow csvRow = readSingleRow("foo");
        assertThrows(IndexOutOfBoundsException.class, () -> csvRow.getField(1));
    }

    // enclosure escaping

    @Test
    public void escapedQuote() {
        assertEquals("bar \"is\" ok",
            readSingleRow("foo,\"bar \"\"is\"\" ok\"").getField(1));
    }

    @Test
    public void handlesEmptyQuotedFieldsAtEndOfRow() {
        assertEquals("", readSingleRow("foo,\"\"").getField(1));
    }

    @Test
    public void dataAfterNewlineAfterEnclosure() {
        List<CsvRow> csv = readAll("\"foo\"\nbar");
        assertEquals(2, csv.size());
        assertEquals("foo", csv.get(0).getField(0));
        assertEquals("bar", csv.get(1).getField(0));

        csv = readAll("\"foo\"\rbar");
        assertEquals(2, csv.size());
        assertEquals("foo", csv.get(0).getField(0));
        assertEquals("bar", csv.get(1).getField(0));

        csv = readAll("\"foo\"\r\nbar");
        assertEquals(2, csv.size());
        assertEquals("foo", csv.get(0).getField(0));
        assertEquals("bar", csv.get(1).getField(0));
    }

    @Test
    public void invalidQuotes() {
        assertArrayEquals(asArray(
            "bbb\"a\"",
            " ccc",
            "ddd\"a",
            "b\"eee",
            "fff",
            "ggg\"a\"\"b",
            ",a, b"
            ),
            readSingleRow("bbb\"a\", ccc,ddd\"a,b\"eee,fff,ggg\"a\"\"b,\",a, b").getFields());
    }

    @Test
    public void textBeforeQuotes() {
        assertArrayEquals(asArray("a\"b\"", "c"), readSingleRow("a\"b\",c").getFields());
    }

    @Test
    public void textAfterQuotes() {
        assertArrayEquals(asArray("ab", "c"), readSingleRow("\"a\"b,c").getFields());
    }

    @Test
    public void spaceBeforeQuotes() {
        assertArrayEquals(asArray(" \"a\"", "b"), readSingleRow(" \"a\",b").getFields());
    }

    @Test
    public void spaceAfterQuotes() {
        assertArrayEquals(asArray("a ", "b"), readSingleRow("\"a\" ,b").getFields());
    }

    @Test
    public void openingQuotes() {
        assertEquals("aaa", readSingleRow("\"aaa").getField(0));
    }

    @Test
    public void closingQuotes() {
        assertEquals("aaa\"", readSingleRow("aaa\"").getField(0));
    }

    // line breaks

    @Test
    public void lineFeed() {
        final Iterator<CsvRow> it = parse("foo\nbar").iterator();
        assertEquals("foo", it.next().getField(0));
        assertEquals("bar", it.next().getField(0));
        assertFalse(it.hasNext());
    }

    @Test
    public void carriageReturn() {
        final Iterator<CsvRow> it = parse("foo\rbar").iterator();
        assertEquals("foo", it.next().getField(0));
        assertEquals("bar", it.next().getField(0));
        assertFalse(it.hasNext());
    }

    @Test
    public void carriageReturnLineFeed() {
        final Iterator<CsvRow> it = parse("foo\r\nbar").iterator();
        assertEquals("foo", it.next().getField(0));
        assertEquals("bar", it.next().getField(0));
        assertFalse(it.hasNext());
    }

    // line numbering

    @Test
    public void lineNumbering() {
        final Iterator<CsvRow> it = parse(
            "line 1\n"
                + "line 2\r"
                + "line 3\r\n"
                + "\"line 4\rwith\r\nand\n\"\n"
                + "line 8"
        ).iterator();

        CsvRow row = it.next();
        assertArrayEquals(asArray("line 1"), row.getFields());
        assertEquals(1, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 2"), row.getFields());
        assertEquals(2, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 3"), row.getFields());
        assertEquals(3, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 4\rwith\r\nand\n"), row.getFields());
        assertEquals(4, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 8"), row.getFields());
        assertEquals(8, row.getOriginalLineNumber());

        assertFalse(it.hasNext());
    }

    // to string

    @Test
    public void toStringWithoutHeader() {
        assertEquals("CsvRowImpl[originalLineNumber=1, fields=[fieldA, fieldB]]",
            readSingleRow("fieldA,fieldB\n").toString());
    }

    // test helpers

    private CsvReader parse(final String data) {
        return crb.build(new StringReader(data));
    }

    private CsvRow readSingleRow(final String data) {
        final List<CsvRow> lists = readAll(data);
        assertEquals(1, lists.size());
        return lists.get(0);
    }

    private List<CsvRow> readAll(final String data) {
        return parse(data)
            .stream()
            .collect(Collectors.toList());
    }

}
