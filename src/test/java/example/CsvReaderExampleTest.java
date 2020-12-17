package example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

public class CsvReaderExampleTest {

    @Test
    public void simple() {
        final Iterator<CsvRow> csv = CsvReader.builder()
            .build("foo,bar")
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
            .build("foo;bar")
            .iterator();

        assertArrayEquals(asArray("foo", "bar"), csv.next().getFields());
        assertFalse(csv.hasNext());
    }

    @Test
    public void header() {
        final Iterator<NamedCsvRow> csv = NamedCsvReader.builder()
            .build("header1,header2\nvalue1,value2").iterator();

        final NamedCsvRow row = csv.next();
        assertEquals("value2", row.getField("header2"));
    }

    @Test
    public void stream() {
        final long streamCount = CsvReader.builder()
            .build("foo\nbar")
            .stream()
            .count();

        assertEquals(2, streamCount);
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Charset charset = StandardCharsets.UTF_8;

        final Path path = tempDir.resolve("fastcsv.csv");
        Files.write(path, "foo,bar\n".getBytes(charset));

        try (CsvReader csvReader = CsvReader.builder().build(path, charset)) {
            for (CsvRow row : csvReader) {
                assertArrayEquals(asArray("foo", "bar"), row.getFields());
            }
        }
    }

}