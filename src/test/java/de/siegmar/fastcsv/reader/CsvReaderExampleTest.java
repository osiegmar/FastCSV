package de.siegmar.fastcsv.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

public class CsvReaderExampleTest {

    @Test
    public void simple() {
        final Iterator<CsvRow> csv = CsvReader.builder()
            .build(new StringReader("foo,bar"))
            .iterator();

        assertArrayEquals(asArray("foo", "bar"), csv.next().getFields());
        assertFalse(csv.hasNext());
    }

    private static String[] asArray(final String... items) {
        return items;
    }

    @Test
    public void configuration() {
        final Iterator<CsvRow> csv = CsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .skipEmptyRows(true)
            .errorOnDifferentFieldCount(true)
            .build(new StringReader("foo;bar"))
            .iterator();

        assertArrayEquals(asArray("foo", "bar"), csv.next().getFields());
        assertFalse(csv.hasNext());
    }

    @Test
    public void header() {
        final Iterator<NamedCsvRow> csv = CsvReader.builder()
            .build(new StringReader("header1,header2\nvalue1,value2"))
            .withHeader()
            .iterator();

        assertEquals("value2", csv.next().getField("header2"));
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
                assertArrayEquals(asArray("foo", "bar"), row.getFields());
            }
        }
    }

}
