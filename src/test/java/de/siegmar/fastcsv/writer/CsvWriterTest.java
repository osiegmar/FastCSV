package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CsvWriterTest {

    private final CsvWriterBuilder csvWriter = CsvWriter.builder().lineDelimiter(LineDelimiter.LF);

    @Test
    public void nullQuote() throws IOException {
        assertEquals("foo,,bar\n", write("foo", null, "bar"));
        assertEquals("foo,,bar\n", write("foo", "", "bar"));
        assertEquals("foo,\",\",bar\n", write("foo", ",", "bar"));
    }

    @Test
    public void emptyQuote() throws IOException {
        csvWriter.quoteStrategy(QuoteStrategy.EMPTY);
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
        csvWriter.quoteStrategy(QuoteStrategy.ALWAYS);
        assertEquals("\"a\",\"b,c\",\"d\ne\",\"f\"\"g\",\"\",\"\"\n",
            write("a", "b,c", "d\ne", "f\"g", "", null));
    }

    @Test
    public void fieldSeparator() throws IOException {
        csvWriter.fieldSeparator(';');
        assertEquals("foo;bar\n", write("foo", "bar"));
    }

    @Test
    public void quoteCharacter() throws IOException {
        csvWriter.quoteCharacter('\'');
        assertEquals("'foo,bar'\n", write("foo,bar"));
    }

    @Test
    public void escapeQuotes() throws IOException {
        assertEquals("foo,\"\"\"bar\"\"\"\n", write("foo", "\"bar\""));
    }

    @Test
    public void appending() throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter appender = csvWriter.build(sw);
        appender.writeField("foo");
        appender.writeField("bar");
        assertEquals("foo,bar", sw.toString());
    }

    private String write(final String... cols) throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = csvWriter.build(sw);
        to.writeLine(cols);
        return sw.toString();
    }

    private String write(final List<String> cols) throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter to = csvWriter.build(sw);
        to.writeLine(cols);
        return sw.toString();
    }

}
