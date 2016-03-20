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

package de.siegmar.fastcsv.writer;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class CsvWriterTest {

    private CsvWriter csvWriter;

    @BeforeMethod
    public void init() {
        csvWriter = new CsvWriter();
        csvWriter.setLineDelimiter(new char[] {'\n'});
    }

    public void oneLineSingleValue() throws IOException {
        assertEquals(write("foo"), "foo\n");
    }

    public void oneLineTwoValues() throws IOException {
        assertEquals(write("foo", "bar"), "foo,bar\n");
    }

    public void twoLinesSingleValue() throws IOException {
        final Collection<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"foo"});
        rows.add(new String[] {"bar"});

        assertEquals(write(rows), "foo\nbar\n");
    }

    public void twoLinesTwoValues() throws IOException {
        assertEquals(write("foo", "bar"), "foo,bar\n");
    }

    public void delimitText() throws IOException {
        assertEquals(write("a", "b,c", "d\ne", "f\"g", "", null),
            "a,\"b,c\",\"d\ne\",\"f\"\"g\",,\n");
    }

    public void alwaysDelimitText() throws IOException {
        csvWriter.setAlwaysDelimitText(true);
        assertEquals(write("a", "b,c", "d\ne", "f\"g", "", null),
            "\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",\"\",\"\"\n");
    }

    public void fieldSeparator() throws IOException {
        csvWriter.setFieldSeparator(';');
        assertEquals(write("foo", "bar"), "foo;bar\n");
    }

    public void textDelimiter() throws IOException {
        csvWriter.setTextDelimiter('\'');
        assertEquals(write("foo,bar"), "'foo,bar'\n");
    }

    public void appending() throws IOException {
        final StringWriter sw = new StringWriter();
        try (final CsvAppender appender = csvWriter.append(sw)) {
            appender.appendField("foo");
            appender.appendField("bar");
        }
        assertEquals(sw.toString(), "foo,bar");
    }

    private String write(final String... cols) throws IOException {
        final Collection<String[]> rows = new ArrayList<>();
        rows.add(cols);

        return write(rows);
    }

    private String write(final Collection<String[]> rows) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        csvWriter.write(stringWriter, rows);

        return stringWriter.toString();
    }


}
