import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data to a file.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final Path file = Files.createTempFile("fastcsv", ".csv");
    file.toFile().deleteOnExit();

    try (CsvWriter csv = CsvWriter.builder().build(file)) {
        csv
            .writeRecord("header1", "header2")
            .writeRecord("value1", "value2");
    }

    Files.readAllLines(file, StandardCharsets.UTF_8)
        .forEach(IO::println);
}
