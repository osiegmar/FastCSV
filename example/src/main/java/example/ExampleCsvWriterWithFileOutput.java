package example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data to a file.
public class ExampleCsvWriterWithFileOutput {

    public static void main(final String[] args) throws IOException {
        final Path file = Files.createTempFile("fastcsv", ".csv");
        file.toFile().deleteOnExit();

        try (CsvWriter csv = CsvWriter.builder().build(file)) {
            csv
                .writeRecord("header1", "header2")
                .writeRecord("value1", "value2");
        }

        Files.readAllLines(file, StandardCharsets.UTF_8)
            .forEach(System.out::println);
    }

}
