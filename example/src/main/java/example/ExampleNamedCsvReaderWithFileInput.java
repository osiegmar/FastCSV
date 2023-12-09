package example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

/**
 * Example for reading CSV data from a file with a header.
 */
public class ExampleNamedCsvReaderWithFileInput {

    private static final String DATA = "header1,header2\nfoo,bar\nfoo2,bar2";

    public static void main(final String[] args) throws IOException {
        final Path tmpFile = prepareTestFile();

        System.out.println("Reading data via for-each loop:");
        try (NamedCsvReader csv = NamedCsvReader.from(CsvReader.builder().build(tmpFile))) {
            for (final NamedCsvRecord csvRecord : csv) {
                System.out.println(csvRecord.getFieldsAsMap());
            }
        }

        System.out.println("Reading data via forEach lambda:");
        try (NamedCsvReader csv = NamedCsvReader.from(CsvReader.builder().build(tmpFile))) {
            csv.forEach(rec -> System.out.println(rec.getField("header2")));
        }

        System.out.println("Reading data via stream:");
        try (Stream<NamedCsvRecord> stream = NamedCsvReader.from(CsvReader.builder().build(tmpFile)).stream()) {
            stream
                .map(rec -> rec.getField("header2"))
                .forEach(System.out::println);
        }

        System.out.println("Reading data via iterator:");
        try (CsvReader csvReader = CsvReader.builder().build(tmpFile);
             CloseableIterator<NamedCsvRecord> iterator = NamedCsvReader.from(csvReader).iterator()) {
            while (iterator.hasNext()) {
                final NamedCsvRecord csvRecord = iterator.next();
                System.out.println(csvRecord.getFieldsAsMap());
            }
        }
    }

    private static Path prepareTestFile() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();
        Files.writeString(tmpFile, DATA, StandardCharsets.UTF_8);
        return tmpFile;
    }

}
