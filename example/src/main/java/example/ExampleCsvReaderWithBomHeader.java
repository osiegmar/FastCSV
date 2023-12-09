package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/**
 * Example for reading CSV files with a BOM header.
 */
public class ExampleCsvReaderWithBomHeader {

    public static void main(final String[] args) throws IOException {
        final Path testFile = prepareTestFile();
        final CsvReader.CsvReaderBuilder builder = CsvReader.builder().detectBomHeader(true);
        try (Stream<CsvRecord> stream = builder.build(testFile).stream()) {
            stream.forEach(System.out::println);
        }
    }

    // Create a test file with a UTF-8 BOM header
    private static Path prepareTestFile() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();
        Files.write(tmpFile, new byte[]{
            (byte) 0xef, (byte) 0xbb, (byte) 0xbf,
            'f', 'o', 'o', ',',
            (byte) 0xc3, (byte) 0xa4,
            (byte) 0xc3, (byte) 0xb6,
            (byte) 0xc3, (byte) 0xbc});
        return tmpFile;
    }

}
