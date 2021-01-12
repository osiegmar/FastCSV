package blackbox.writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
public class CsvWriterTest {

    private final CsvWriter.CsvWriterBuilder crw = CsvWriter.builder()
        .lineDelimiter(LineDelimiter.LF);

    @ParameterizedTest
    @ValueSource(chars = {'\r', '\n'})
    public void configBuilder(final char c) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            CsvWriter.builder().fieldSeparator(c).build(new StringWriter()));
        assertEquals("fieldSeparator must not be a newline char", e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
            CsvWriter.builder().quoteCharacter(c).build(new StringWriter()));
        assertEquals("quoteCharacter must not be a newline char", e2.getMessage());
    }

    @Test
    public void configWriter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            crw.fieldSeparator(',').quoteCharacter(',').build(new StringWriter()));
        assertTrue(e.getMessage().contains("Control characters must differ"));
    }

    @Test
    public void nullQuote() {
        assertEquals("foo,,bar\n", write("foo", null, "bar"));
        assertEquals("foo,,bar\n", write("foo", "", "bar"));
        assertEquals("foo,\",\",bar\n", write("foo", ",", "bar"));
    }

    @Test
    public void emptyQuote() {
        crw.quoteStrategy(QuoteStrategy.EMPTY);
        assertEquals("foo,,bar\n", write("foo", null, "bar"));
        assertEquals("foo,\"\",bar\n", write("foo", "", "bar"));
        assertEquals("foo,\",\",bar\n", write("foo", ",", "bar"));
    }

    @Test
    public void oneLineSingleValue() {
        assertEquals("foo\n", write("foo"));
    }

    @Test
    public void oneLineTwoValues() {
        assertEquals("foo,bar\n", write("foo", "bar"));
    }

    @Test
    public void oneLineTwoValuesAsList() {
        final List<String> cols = new ArrayList<>();
        cols.add("foo");
        cols.add("bar");

        final StringWriter sw = new StringWriter();
        crw.build(sw)
            .writeRow(cols)
            .writeRow(cols);

        assertEquals("foo,bar\nfoo,bar\n", sw.toString());
    }

    @Test
    public void twoLinesTwoValues() {
        assertEquals("foo,bar\n", write("foo", "bar"));
    }

    @Test
    public void delimitText() {
        assertEquals("a,\"b,c\",\"d\ne\",\"f\"\"g\",,\n",
            write("a", "b,c", "d\ne", "f\"g", "", null));
    }

    @Test
    public void alwaysQuoteText() {
        crw.quoteStrategy(QuoteStrategy.ALWAYS);
        assertEquals("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",\"\",\"\"\n",
            write("a", "b,c", "d\ne", "f\"g", "", null));
    }

    @Test
    public void fieldSeparator() {
        crw.fieldSeparator(';');
        assertEquals("foo;bar\n", write("foo", "bar"));
    }

    @Test
    public void quoteCharacter() {
        crw.quoteCharacter('\'');
        assertEquals("'foo,bar'\n", write("foo,bar"));
    }

    @Test
    public void escapeQuotes() {
        assertEquals("foo,\"\"\"bar\"\"\"\n", write("foo", "\"bar\""));
    }

    @Test
    public void appending() {
        final StringWriter sw = new StringWriter();
        final CsvWriter appender = crw.build(sw);
        appender.writeRow("foo", "bar").writeRow("foo2", "bar2");
        assertEquals("foo,bar\nfoo2,bar2\n", sw.toString());
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        try (CsvWriter csv = CsvWriter.builder().build(file, UTF_8)) {
            csv.writeRow("value1", "value2");
        }

        assertEquals("value1,value2\r\n",
            new String(Files.readAllBytes(file), UTF_8));
    }

    @Test
    public void chained() {
        final CsvWriter writer = CsvWriter.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .quoteStrategy(QuoteStrategy.REQUIRED)
            .lineDelimiter(LineDelimiter.CRLF)
            .build(new StringWriter());

        assertNotNull(writer);
    }

    @Test
    public void streaming() {
        final Stream<String[]> stream = Stream.of(
            new String[]{"header1", "header2"},
            new String[]{"value1", "value2"}
        );
        final StringWriter sw = new StringWriter();
        final CsvWriter csvWriter = CsvWriter.builder().build(sw);
        stream.forEach(csvWriter::writeRow);
        assertEquals("header1,header2\r\nvalue1,value2\r\n", sw.toString());
    }

    @Test
    public void mixedWriterUsage() {
        final StringWriter stringWriter = new StringWriter();
        final CsvWriter csvWriter = CsvWriter.builder().build(stringWriter);
        csvWriter.writeRow("foo", "bar");
        stringWriter.write("# my comment\r\n");
        csvWriter.writeRow("1", "2");
        assertEquals("foo,bar\r\n# my comment\r\n1,2\r\n", stringWriter.toString());
    }

    @Test
    public void unwritableArray() {
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () ->
            crw.build(new UnwritableWriter()).writeRow("foo"));

        assertEquals("java.io.IOException: Cannot write", e.getMessage());
    }

    @Test
    public void unwritableIterable() {
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () ->
            crw.build(new UnwritableWriter()).writeRow(Collections.singletonList("foo")));

        assertEquals("java.io.IOException: Cannot write", e.getMessage());
    }

    private String write(final String... cols) {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = crw.build(sw);
        to.writeRow(cols);
        return sw.toString();
    }

}
