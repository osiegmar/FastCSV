import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing and reading compressed CSV data.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final Path file = Files.createTempFile("fastcsv", ".csv.gz");
    file.toFile().deleteOnExit();

    writeCsvGzipped(file);
    readCsvGzipped(file);
}

void writeCsvGzipped(final Path file) throws IOException {
    IO.println("Writing compressed CSV file: " + file);

    try (
        var csv = CsvWriter.builder()
            .build(new GZIPOutputStream(Files.newOutputStream(file)))
    ) {
        csv.writeRecord("header1", "header2");
        csv.writeRecord("value1", "value2");
    }
}

void readCsvGzipped(final Path file) throws IOException {
    IO.println("Reading compressed CSV file: " + file);

    try (
        var csv = CsvReader.builder()
            .ofCsvRecord(new GZIPInputStream(Files.newInputStream(file)))
    ) {
        csv.forEach(IO::println);
    }
}
