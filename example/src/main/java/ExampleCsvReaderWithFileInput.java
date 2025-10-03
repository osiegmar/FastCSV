import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/// Example for reading CSV data from a file.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final String data = """
        foo,bar
        foo2,bar2
        """;

    final Path tmpFile = prepareTestFile(data);

    IO.println("Reading data via for-each loop:");
    try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(tmpFile)) {
        for (final CsvRecord csvRecord : csv) {
            IO.println(csvRecord.getFields());
        }
    }

    IO.println("Reading data via forEach lambda:");
    try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(tmpFile)) {
        csv.forEach(IO::println);
    }

    IO.println("Reading data via stream:");
    try (Stream<CsvRecord> stream = CsvReader.builder().ofCsvRecord(tmpFile).stream()) {
        stream
            .map(rec -> rec.getField(1))
            .forEach(IO::println);
    }

    IO.println("Reading data via iterator:");
    try (CloseableIterator<CsvRecord> it = CsvReader.builder().ofCsvRecord(tmpFile).iterator()) {
        while (it.hasNext()) {
            final CsvRecord csvRecord = it.next();
            IO.println(csvRecord.getFields());
        }
    }
}

Path prepareTestFile(final String data) throws IOException {
    final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
    tmpFile.toFile().deleteOnExit();
    Files.writeString(tmpFile, data, StandardCharsets.UTF_8);
    return tmpFile;
}
