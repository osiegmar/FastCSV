package blackbox.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategies;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
class CsvWriterTest {

    private final CsvWriter.CsvWriterBuilder crw = CsvWriter.builder()
        .lineDelimiter(LineDelimiter.LF)
        .bufferSize(0);

    @ParameterizedTest
    @ValueSource(chars = {'\r', '\n'})
    void configBuilder(final char c) {
        assertThatThrownBy(() -> CsvWriter.builder().fieldSeparator(c).build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("fieldSeparator must not be a newline char");

        assertThatThrownBy(() -> CsvWriter.builder().quoteCharacter(c).build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("quoteCharacter must not be a newline char");

        assertThatThrownBy(() -> CsvWriter.builder().commentCharacter(c).build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("commentCharacter must not be a newline char");
    }

    @Test
    void configWriter() {
        assertThatThrownBy(() -> CsvWriter.builder()
            .fieldSeparator(',').quoteCharacter(',').build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=#)");

        assertThatThrownBy(() -> CsvWriter.builder()
            .fieldSeparator(',').commentCharacter(',').build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=\", commentCharacter=,)");

        assertThatThrownBy(() -> CsvWriter.builder()
            .quoteCharacter(',').commentCharacter(',').build(new StringWriter()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=,)");
    }

    @Test
    void nullQuote() {
        assertThat(write("foo", null, "bar")).isEqualTo("foo,,bar\n");
        assertThat(write("foo", "", "bar")).isEqualTo("foo,,bar\n");
        assertThat(write("foo", ",", "bar")).isEqualTo("foo,\",\",bar\n");
    }

    @Test
    void emptyQuote() {
        crw.quoteStrategy(QuoteStrategies.EMPTY);
        assertThat(write("foo", null, "bar")).isEqualTo("foo,,bar\n");
        assertThat(write("foo", "", "bar")).isEqualTo("foo,\"\",bar\n");
        assertThat(write("foo", ",", "bar")).isEqualTo("foo,\",\",bar\n");
    }

    @Test
    void oneLineSingleValue() {
        assertThat(write("foo")).isEqualTo("foo\n");
    }

    @Test
    void oneLineTwoValues() {
        assertThat(write("foo", "bar")).isEqualTo("foo,bar\n");
    }

    @Test
    void twoLinesTwoValues() {
        final List<String> cols = new ArrayList<>();
        cols.add("foo");
        cols.add("bar");

        assertThat(write(w -> w.writeRecord(cols).writeRecord(cols)))
            .isEqualTo("foo,bar\nfoo,bar\n");
    }

    @Test
    void delimitText() {
        assertThat(write("a", "b,c", "d\ne", "f\"g", "", null))
            .isEqualTo("a,\"b,c\",\"d\ne\",\"f\"\"g\",,\n");
    }

    @Test
    void alwaysQuoteText() {
        crw.quoteStrategy(QuoteStrategies.ALWAYS);
        assertThat(write("a", "b,c", "d\ne", "f\"g", "", null))
            .isEqualTo("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",\"\",\"\"\n");
    }

    @Test
    void alwaysQuoteTextIgnoreEmpty() {
        crw.quoteStrategy(QuoteStrategies.NON_EMPTY);
        assertThat(write("a", "b,c", "d\ne", "f\"g", "", null))
            .isEqualTo("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",,\n");
    }

    @Test
    void fieldSeparator() {
        crw.fieldSeparator(';');
        assertThat(write("foo", "bar")).isEqualTo("foo;bar\n");
    }

    @Test
    void quoteCharacter() {
        crw.quoteCharacter('\'');
        assertThat(write("foo,bar")).isEqualTo("'foo,bar'\n");
    }

    @Test
    void escapeQuotes() {
        assertThat(write("foo", "\"bar\"")).isEqualTo("foo,\"\"\"bar\"\"\"\n");
    }

    @Test
    void commentCharacter() {
        assertThat(write("#foo", "#bar")).isEqualTo("\"#foo\",#bar\n");
        assertThat(write(" #foo", "#bar")).isEqualTo(" #foo,#bar\n");
    }

    @Test
    void commentCharacterDifferentChar() {
        assertThat(write(";foo", "bar")).isEqualTo(";foo,bar\n");

        crw.commentCharacter(';');
        assertThat(write(";foo", "bar")).isEqualTo("\";foo\",bar\n");
    }

    @Test
    void writeComment() {
        assertThat(write(w -> w.writeComment("this is a comment")))
            .isEqualTo("#this is a comment\n");
    }

    @Test
    void writeCommentWithNewlines() {
        assertThat(write(w -> w.writeComment("\rline 2\nline 3\r\nline 4\n")))
            .isEqualTo("#\n#line 2\n#line 3\n#line 4\n#\n");
    }

    @Test
    void writeEmptyComment() {
        assertThat(write(w -> w.writeComment("").writeComment(null)))
            .isEqualTo("#\n#\n");
    }

    @Test
    void writeCommentDifferentChar() {
        crw.commentCharacter(';');
        assertThat(write(w -> w.writeComment("this is a comment")))
            .isEqualTo(";this is a comment\n");
    }

    @Test
    void appending() {
        assertThat(write(w -> w.writeRecord("foo", "bar").writeRecord("foo2", "bar2")))
            .isEqualTo("foo,bar\nfoo2,bar2\n");
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        try (CsvWriter csv = CsvWriter.builder().build(file)) {
            csv.writeRecord("value1", "value2");
        }

        assertThat(file).hasContent("value1,value2\r\n");
    }

    @Test
    void chained() {
        final CsvWriter writer = CsvWriter.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .quoteStrategy(QuoteStrategies.ALWAYS)
            .lineDelimiter(LineDelimiter.CRLF)
            .build(new StringWriter());

        assertThat(writer).isNotNull();
    }

    @Test
    void streaming() {
        final Stream<String[]> stream = Stream.of(
            new String[]{"header1", "header2"},
            new String[]{"value1", "value2"}
        );
        final StringWriter sw = new StringWriter();
        final CsvWriter csvWriter = CsvWriter.builder().bufferSize(0).build(sw);
        stream.forEach(csvWriter::writeRecord);

        assertThat(sw).asString()
            .isEqualTo("header1,header2\r\nvalue1,value2\r\n");
    }

    @Test
    void mixedWriterUsage() {
        final StringWriter stringWriter = new StringWriter();
        final CsvWriter csvWriter = CsvWriter.builder().bufferSize(0).build(stringWriter);
        csvWriter.writeRecord("foo", "bar");
        stringWriter.write("# my comment\r\n");
        csvWriter.writeRecord("1", "2");

        assertThat(stringWriter).asString()
            .isEqualTo("foo,bar\r\n# my comment\r\n1,2\r\n");
    }

    @Test
    void unwritableArray() {
        assertThatThrownBy(() -> crw.build(new UnwritableWriter()).writeRecord("foo"))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot write");
    }

    @Test
    void unwritableIterable() {
        assertThatThrownBy(() -> crw.build(new UnwritableWriter()).writeRecord(List.of("foo")))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot write");

        assertThatThrownBy(() -> crw.build(new UnwritableWriter()).writeComment("foo"))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot write");
    }

    // buffer

    @Test
    void invalidBuffer() {
        assertThatThrownBy(() -> crw.bufferSize(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disableBuffer() {
        final StringWriter stringWriter = new StringWriter();
        crw.bufferSize(0).build(stringWriter).writeRecord("foo", "bar");

        assertThat(stringWriter).asString().isEqualTo("foo,bar\n");
    }

    // autoFlush

    @Test
    void noAutoFlush() {
        final CsvWriter csvWriter = CsvWriter.builder().build(flushFailWriter());
        assertThatCode(() -> csvWriter.writeRecord("foo"))
            .doesNotThrowAnyException();
    }

    @Test
    void manualFlush(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        CsvWriter.builder().build(file)
            .writeRecord("foo")
            .flush();

        assertThat(Files.readString(file))
            .isEqualTo("foo\r\n");
    }

    @Test
    void autoFlush() {
        final CsvWriter csvWriter = CsvWriter.builder().autoFlush(true).build(flushFailWriter());
        assertThatThrownBy(() -> csvWriter.writeRecord("foo"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private static FilterWriter flushFailWriter() {
        return new FilterWriter(FilterWriter.nullWriter()) {
            @Override
            public void flush() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // toString()

    @Test
    void builderToString() {
        assertThat(crw).asString()
            .isEqualTo("CsvWriterBuilder[fieldSeparator=,, quoteCharacter=\", "
                + "commentCharacter=#, quoteStrategy=null, lineDelimiter=\n, bufferSize=0, autoFlush=false]");
    }

    @Test
    void writerToString() {
        assertThat(crw.build(new StringWriter())).asString()
            .isEqualTo("CsvWriter[fieldSeparator=,, quoteCharacter=\", commentCharacter=#, "
                + "quoteStrategy=null, lineDelimiter='\n']");
    }

    private String write(final String... cols) {
        return write(w -> w.writeRecord(cols));
    }

    private String write(final Consumer<CsvWriter> c) {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = crw.build(sw);
        c.accept(to);
        return sw.toString();
    }

}
