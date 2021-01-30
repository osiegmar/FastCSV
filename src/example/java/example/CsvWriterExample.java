package example;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

@SuppressWarnings("PMD.SystemPrintln")
public class CsvWriterExample {

    public static void main(final String[] args) {
        simple();
        advancedConfiguration();
        file();
        transformData();
    }

    private static void simple() {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw).writeRow("value1", "value2");
        System.out.print("Simple CSV: " + sw);
    }

    private static void advancedConfiguration() {
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

    private static void file() {
        try {
            final Path path = Files.createTempFile("fastcsv", ".csv");

            try (CsvWriter csv = CsvWriter.builder().build(path)) {
                csv
                    .writeRow("header1", "header2")
                    .writeRow("value1", "value2");
            }

            Files.lines(path)
                .forEach(line -> System.out.println("Line from path: " + line));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void transformData() {
        final StringWriter out = new StringWriter();

        try (
            NamedCsvReader reader = NamedCsvReader.builder().build(
                "firstname,lastname,age\njohn,smith,30");
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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("Transformed CSV:");
        System.out.println(out);
    }

}
