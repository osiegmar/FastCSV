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
import java.util.function.Consumer;
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

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
            CsvWriter.builder().commentCharacter(c).build(new StringWriter()));
        assertEquals("commentCharacter must not be a newline char", e3.getMessage());
    }

    @Test
    public void configWriter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            crw.fieldSeparator(',').quoteCharacter(',').build(new StringWriter()));
        assertTrue(e.getMessage().contains("Control characters must differ"));

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
            crw.fieldSeparator(',').commentCharacter(',').build(new StringWriter()));
        assertTrue(e2.getMessage().contains("Control characters must differ"));

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
            crw.quoteCharacter(',').commentCharacter(',').build(new StringWriter()));
        assertTrue(e3.getMessage().contains("Control characters must differ"));
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

        assertEquals("foo,bar\nfoo,bar\n",
            write(w -> w.writeRow(cols).writeRow(cols)));
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
    public void alwaysQuoteTextIgnoreEmpty() {
        crw.quoteStrategy(QuoteStrategy.ALWAYS_NON_EMPTY);
        assertEquals("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",,\n",
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
    public void commentCharacter() {
        assertEquals("\"#foo\",#bar\n", write("#foo", "#bar"));
        assertEquals(" #foo,#bar\n", write(" #foo", "#bar"));
    }

    @Test
    public void commentCharacterDifferentChar() {
        assertEquals(";foo,bar\n", write(";foo", "bar"));

        crw.commentCharacter(';');
        assertEquals("\";foo\",bar\n", write(";foo", "bar"));
    }

    @Test
    public void writeComment() {
        assertEquals("#this is a comment\n", write(w -> w.writeComment("this is a comment")));
    }

    @Test
    public void writeCommentWithNewlines() {
        assertEquals("#\n#line 2\n#line 3\n#line 4\n#\n",
            write(w -> w.writeComment("\rline 2\nline 3\r\nline 4\n")));
    }

    @Test
    public void writeEmptyComment() {
        assertEquals("#\n#\n", write(w -> w.writeComment("").writeComment(null)));
    }

    @Test
    public void writeCommentDifferentChar() {
        crw.commentCharacter(';');
        assertEquals(";this is a comment\n", write(w -> w.writeComment("this is a comment")));
    }

    @Test
    public void appending() {
        assertEquals("foo,bar\nfoo2,bar2\n",
            write(w -> w.writeRow("foo", "bar").writeRow("foo2", "bar2")));
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        try (CsvWriter csv = CsvWriter.builder().build(file)) {
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

        final UncheckedIOException e2 = assertThrows(UncheckedIOException.class, () ->
            crw.build(new UnwritableWriter()).writeComment("foo"));

        assertEquals("java.io.IOException: Cannot write", e2.getMessage());
    }

    // buffer

    @Test
    public void invalidBuffer() {
        assertThrows(IllegalArgumentException.class, () -> crw.bufferSize(-1));
    }

    @Test
    public void disableBuffer() {
        final StringWriter stringWriter = new StringWriter();
        crw.bufferSize(0).build(stringWriter).writeRow("foo", "bar");
        assertEquals("foo,bar\n", stringWriter.toString());
    }

    // toString()

    @Test
    public void builderToString() {
        assertEquals("CsvWriterBuilder[fieldSeparator=,, quoteCharacter=\", "
            + "commentCharacter=#, quoteStrategy=REQUIRED, lineDelimiter=\n, bufferSize=8192]", crw.toString());
    }

    @Test
    public void writerToString() {
        assertEquals("CsvWriter[fieldSeparator=,, quoteCharacter=\", commentCharacter=#, "
            + "quoteStrategy=REQUIRED, lineDelimiter='\n']", crw.build(new StringWriter()).toString());
    }

    private String write(final String... cols) {
        return write(w -> w.writeRow(cols));
    }

    private String write(final Consumer<CsvWriter> c) {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = crw.build(sw);
        c.accept(to);
        return sw.toString();
    }

}
