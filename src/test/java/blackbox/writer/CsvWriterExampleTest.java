package blackbox.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

public class CsvWriterExampleTest {

    @Test
    public void simple() throws IOException {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw).writeLine("value1", "value2");
        assertEquals("value1,value2\r\n", sw.toString());
    }

    @Test
    public void complex() throws IOException {
        final StringWriter sw = new StringWriter();

        CsvWriter.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .quoteStrategy(QuoteStrategy.REQUIRED)
            .lineDelimiter(LineDelimiter.LF)
            .build(sw)
            .writeField("header1").writeField("header2").endLine()
            .writeLine("value1", "value2");

        assertEquals("header1,header2\nvalue1,value2\n", sw.toString());
    }

    @Test
    public void stringWriter() throws IOException {
        final StringWriter sw = new StringWriter();

        CsvWriter.builder()
            .build(sw)
            .writeLine("header1", "header2")
            .writeLine("value1", "value2");

        assertEquals("header1,header2\r\nvalue1,value2\r\n", sw.toString());
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path path = tempDir.resolve("fastcsv.csv");
        final Charset charset = StandardCharsets.UTF_8;

        try (CsvWriter csv = CsvWriter.builder().build(path, charset)) {
            csv
                .writeField("header1").writeField("header2").endLine()
                .writeLine("value1", "value2");
        }

        assertEquals("header1,header2\r\nvalue1,value2\r\n",
            new String(Files.readAllBytes(path), charset));
    }

}
