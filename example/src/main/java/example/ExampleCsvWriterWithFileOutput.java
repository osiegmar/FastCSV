package example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for writing CSV data to a file.
 */
public class ExampleCsvWriterWithFileOutput {

    public static void main(final String[] args) throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");
        path.toFile().deleteOnExit();

        try (CsvWriter csv = CsvWriter.builder().build(path)) {
            csv
                .writeRecord("header1", "header2")
                .writeRecord("value1", "value2");
        }

        Files.readAllLines(path, StandardCharsets.UTF_8)
            .forEach(System.out::println);
    }

}
