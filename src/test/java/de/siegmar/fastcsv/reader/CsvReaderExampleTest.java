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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CsvReaderExampleTest {

    @Test
    public void simple() {
        final Iterator<CsvRow> csv = CsvReader.builder()
            .build(new StringReader("foo,bar"))
            .iterator();

        assertEquals(Arrays.asList("foo", "bar"), csv.next().getFields());
        assertFalse(csv.hasNext());
    }

    @Test
    public void configuration() {
        final Iterator<CsvRow> csv = CsvReader.builder()
            .fieldSeparator(';')
            .textDelimiter('"')
            .skipEmptyRows(true)
            .errorOnDifferentFieldCount(true)
            .build(new StringReader("foo;bar"))
            .iterator();

        assertEquals(Arrays.asList("foo", "bar"), csv.next().getFields());
        assertFalse(csv.hasNext());
    }

    @Test
    public void header() {
        final Iterator<NamedCsvRow> csv = CsvReader.builder()
            .build(new StringReader("header1,header2\nvalue1,value2"))
            .withHeader()
            .iterator();

        assertEquals(Optional.of("value2"), csv.next().getField("header2"));
    }

    @Test
    public void stream() {
        final long streamCount = CsvReader.builder()
            .build(new StringReader("foo\nbar"))
            .stream()
            .count();

        assertEquals(2, streamCount);
    }

    @Test
    public void path() throws IOException {
        final Charset charset = StandardCharsets.UTF_8;

        final Path path = Files.createTempFile("fastcsv", ".csv");
        Files.write(path, "foo,bar\n".getBytes(charset));

        try (CsvReader csvReader = CsvReader.builder().build(path, charset)) {
            for (CsvRow row : csvReader) {
                assertEquals(Arrays.asList("foo", "bar"), row.getFields());
            }
        }
    }

}
