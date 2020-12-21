package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

public class CsvWriterExample {

    public static void main(final String[] args) throws IOException {
        simple();
        advancedConfiguration();
        path();
    }

    private static void simple() throws IOException {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw).writeRow("value1", "value2");
        System.out.print("Simple CSV: " + sw);
    }

    private static void advancedConfiguration() throws IOException {
        final StringWriter sw = new StringWriter();

        CsvWriter.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .quoteStrategy(QuoteStrategy.ALWAYS)
            .lineDelimiter(LineDelimiter.LF)
            .build(sw)
            .writeField("header1").writeField("header2").endRow()
            .writeRow("value1", "value2");

        System.out.println("Advanced CSV:");
        System.out.println(sw);
    }

    private static void path() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");

        try (CsvWriter csv = CsvWriter.builder().build(path, UTF_8)) {
            csv
                .writeField("header1").writeField("header2").endRow()
                .writeRow("value1", "value2");
        }

        Files.lines(path)
            .forEach(line -> System.out.println("Line from path: " + line));
    }

}
