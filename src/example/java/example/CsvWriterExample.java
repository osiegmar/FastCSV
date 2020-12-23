package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

public class CsvWriterExample {

    public static void main(final String[] args) throws IOException {
        simple();
        advancedConfiguration();
        path();
        transformData();
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
            .writeRow("header1", "header2")
            .writeRow("value1", "value2");

        System.out.println("Advanced CSV:");
        System.out.println(sw);
    }

    private static void path() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");

        try (CsvWriter csv = CsvWriter.builder().build(path, UTF_8)) {
            csv
                .writeRow("header1", "header2")
                .writeRow("value1", "value2");
        }

        Files.lines(path)
            .forEach(line -> System.out.println("Line from path: " + line));
    }

    private static void transformData() throws IOException {
        final String in = "firstname,lastname,age\njohn,smith,30";
        final StringWriter out = new StringWriter();

        try (
            NamedCsvReader reader = NamedCsvReader.builder().build(in);
            CsvWriter writer = CsvWriter.builder().build(out)
        ) {
            // transform firstname,lastname,age => name,age
            writer.writeRow("name", "age");
            for (final NamedCsvRow csvRow : reader) {
                writer.writeRow(
                    csvRow.getField("firstname") + " " + csvRow.getField("lastname"),
                    csvRow.getField("age")
                );
            }
        }

        System.out.println("Transformed CSV:");
        System.out.println(out);
    }

}
