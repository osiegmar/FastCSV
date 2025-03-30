package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for reading CSV files with a BOM header.
public class ExampleCsvReaderWithBomHeader {

    public static void main(final String[] args) throws IOException {
        final Path testFile = prepareTestFile();

        final CsvReader.CsvReaderBuilder builder = CsvReader.builder()
            .detectBomHeader(true);

        try (Stream<CsvRecord> csv = builder.ofCsvRecord(testFile).stream()) {
            csv.forEach(System.out::println);
        }
    }

    // Create a file with UTF-8 encoding and a corresponding BOM header
    static Path prepareTestFile() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();

        try (var out = Files.newOutputStream(tmpFile);
             var csv = CsvWriter.builder().build(out, UTF_8)) {

            // Manually write UTF-8 BOM header
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            csv.writeRecord("a", "o", "u");
            csv.writeRecord("ä", "ö", "ü");
        }

        return tmpFile;
    }

}
