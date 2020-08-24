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

package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class CsvWriterExampleTest {

    @Test
    public void simple() throws IOException {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().to(sw).writeLine("foo", "bar");
        assertEquals("foo,bar\r\n", sw.toString());
    }

    @Test
    public void complex() throws IOException {
        final StringWriter sw = new StringWriter();

        CsvWriter.builder()
            .fieldSeparator(',')
            .textDelimiter('"')
            .textDelimitStrategy(TextDelimitStrategy.REQUIRED)
            .lineDelimiter("\n")
            .to(sw)
            .writeField("foo").writeField("bar").endLine()
            .writeLine("foo1", "bar1");

        assertEquals("foo,bar\nfoo1,bar1\n", sw.toString());
    }

    @Test
    public void stringWriter() throws IOException {
        final StringWriter sw = new StringWriter();

        CsvWriter.builder()
            .to(sw)
            .writeLine("foo", "bar")
            .writeLine("foo1", "bar1");

        assertEquals("foo,bar\r\nfoo1,bar1\r\n", sw.toString());
    }

    @Test
    public void path() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");
        final Charset charset = StandardCharsets.UTF_8;

        try (CloseableCsvWriter csv = CsvWriter.builder().to(path, charset)) {
            csv.writeLine("foo", "bar").writeLine("foo1", "bar1");
        }

        assertEquals("foo,bar\r\nfoo1,bar1\r\n",
            new String(Files.readAllBytes(path), charset));
    }

    @Test
    public void file() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");
        final File file = path.toFile();
        final Charset charset = StandardCharsets.UTF_8;
        final boolean append = false;

        try (CloseableCsvWriter csv = CsvWriter.builder().to(file, charset, append)) {
            csv.writeLine("foo", "bar").writeLine("foo1", "bar1");
        }

        assertEquals("foo,bar\r\nfoo1,bar1\r\n",
            new String(Files.readAllBytes(path), charset));
    }

}
