package example;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;

@SuppressWarnings("PMD.SystemPrintln")
public class CsvWriterExample {

    public static void main(final String[] args) throws IOException {
        simple();
        advancedConfiguration();
        file();
        transformData();
    }

    private static void simple() {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw).writeRecord("value1", "value2");
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
            .writeComment("File created by foo on 2021-02-07")
            .writeRecord("header1", "header2")
            .writeRecord("value1", "value2");

        System.out.println("Advanced CSV:");
        System.out.println(sw);
    }

    private static void file() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");

        try (CsvWriter csv = CsvWriter.builder().build(path)) {
            csv
                .writeRecord("header1", "header2")
                .writeRecord("value1", "value2");
        }

        Files.lines(path)
            .forEach(line -> System.out.println("Line from path: " + line));
    }

    private static void transformData() throws IOException {
        final StringWriter out = new StringWriter();

        try (
            NamedCsvReader reader = NamedCsvReader.from(CsvReader.builder().build(
                "firstname,lastname,age\njohn,smith,30"));
            CsvWriter writer = CsvWriter.builder().build(out)
        ) {
            // transform firstname,lastname,age => name,age
            writer.writeRecord("name", "age");
            for (final NamedCsvRecord csvRecord : reader) {
                writer.writeRecord(
                    csvRecord.getField("firstname") + " " + csvRecord.getField("lastname"),
                    csvRecord.getField("age")
                );
            }
        }

        System.out.println("Transformed CSV:");
        System.out.println(out);
    }

}
