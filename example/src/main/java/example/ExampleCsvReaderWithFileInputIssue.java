package example;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Example for reading CSV data from a file.
 */
@SuppressWarnings("RedundantExplicitVariableType")
public class ExampleCsvReaderWithFileInputIssue {

    private static final String DATA = "foo,bar\nfoo2,bar2";

    public static void main(final String[] args) throws IOException {
        final Path tmpFile = prepareTestFile();

        System.out.println("Writing data:1 is success");
        try (CsvReader<CsvRecord> records = CsvReader.builder().ofCsvRecord(tmpFile)) {
            for (CsvRecord r : records) {
                System.out.println(r.getFields());
            }
            printRecords(records);
        }
    }

    private static void printRecords(CsvReader<CsvRecord> records) {
        System.out.println("Writing data:2 but empty!!!!");
        for (CsvRecord r : records) {
            System.out.println(r.getFields());
        }
    }

    private static Path prepareTestFile() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();
        Files.writeString(tmpFile, DATA, StandardCharsets.UTF_8);
        return tmpFile;
    }

}
