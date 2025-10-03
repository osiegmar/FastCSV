import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for reading CSV files with a BOM header.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final Path testFile = createTempFile();
    writeTestFile(testFile);
    readTestFile(testFile);
}

Path createTempFile() throws IOException {
    final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
    tmpFile.toFile().deleteOnExit();
    return tmpFile;
}

void writeTestFile(final Path file) throws IOException {
    try (var out = Files.newOutputStream(file);
         var csv = CsvWriter.builder().build(out, UTF_8)) {

        // Manually write UTF-8 BOM header
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        csv.writeRecord("a", "o", "u");
        csv.writeRecord("ä", "ö", "ü");
    }
}

void readTestFile(final Path file) throws IOException {
    final CsvReader.CsvReaderBuilder builder = CsvReader.builder()
        .detectBomHeader(true);

    try (CsvReader<CsvRecord> csv = builder.ofCsvRecord(file)) {
        csv.forEach(IO::println);
    }
}
