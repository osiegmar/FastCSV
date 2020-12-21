package blackbox.writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

public class CsvWriterTest {

    private final CsvWriter.CsvWriterBuilder crw = CsvWriter.builder()
        .lineDelimiter(LineDelimiter.LF);

    @Test
    public void nullQuote() throws IOException {
        assertEquals("foo,,bar\n", write("foo", null, "bar"));
        assertEquals("foo,,bar\n", write("foo", "", "bar"));
        assertEquals("foo,\",\",bar\n", write("foo", ",", "bar"));
    }

    @Test
    public void emptyQuote() throws IOException {
        crw.quoteStrategy(QuoteStrategy.EMPTY);
        assertEquals("foo,,bar\n", write("foo", null, "bar"));
        assertEquals("foo,\"\",bar\n", write("foo", "", "bar"));
        assertEquals("foo,\",\",bar\n", write("foo", ",", "bar"));
    }

    @Test
    public void oneLineSingleValue() throws IOException {
        assertEquals("foo\n", write("foo"));
    }

    @Test
    public void oneLineTwoValues() throws IOException {
        assertEquals("foo,bar\n", write("foo", "bar"));
    }

    @Test
    public void oneLineTwoValuesAsList() throws IOException {
        final List<String> cols = new ArrayList<>();
        cols.add("foo");
        cols.add("bar");
        assertEquals("foo,bar\n", write(cols));
    }

    @Test
    public void twoLinesTwoValues() throws IOException {
        assertEquals("foo,bar\n", write("foo", "bar"));
    }

    @Test
    public void delimitText() throws IOException {
        assertEquals("a,\"b,c\",\"d\ne\",\"f\"\"g\",,\n",
            write("a", "b,c", "d\ne", "f\"g", "", null));
    }

    @Test
    public void alwaysQuoteText() throws IOException {
        crw.quoteStrategy(QuoteStrategy.ALWAYS);
        assertEquals("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",\"\",\"\"\n",
            write("a", "b,c", "d\ne", "f\"g", "", null));
    }

    @Test
    public void fieldSeparator() throws IOException {
        crw.fieldSeparator(';');
        assertEquals("foo;bar\n", write("foo", "bar"));
    }

    @Test
    public void quoteCharacter() throws IOException {
        crw.quoteCharacter('\'');
        assertEquals("'foo,bar'\n", write("foo,bar"));
    }

    @Test
    public void escapeQuotes() throws IOException {
        assertEquals("foo,\"\"\"bar\"\"\"\n", write("foo", "\"bar\""));
    }

    @Test
    public void appending() throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter appender = crw.build(sw);
        appender.writeField("foo").writeField("bar");
        assertEquals("foo,bar", sw.toString());
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        try (CsvWriter csv = CsvWriter.builder().build(file, UTF_8)) {
            csv.writeField("value1").writeRow("value2");
        }

        assertEquals("value1,value2\r\n",
            new String(Files.readAllBytes(file), UTF_8));
    }

    @Test
    public void path(@TempDir final File tempDir) throws IOException {
        final File file = new File(tempDir, "fastcsv.csv");
        try (CsvWriter csv = CsvWriter.builder().build(file, false, UTF_8)) {
            csv.writeField("value1").writeRow("value2");
        }

        assertEquals("value1,value2\r\n",
            new String(Files.readAllBytes(file.toPath()), UTF_8));
    }

    private String write(final String... cols) throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = crw.build(sw);
        to.writeRow(cols);
        return sw.toString();
    }

    private String write(final List<String> cols) throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = crw.build(sw);
        to.writeRow(cols);
        return sw.toString();
    }

}
