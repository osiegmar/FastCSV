package example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/// Example for reading CSV data from a file in the classpath.
public class ExampleCsvReaderWithClasspathInput {

    public static void main(final String[] args) throws IOException {
        try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(readFromClasspath("/example.csv"))) {
            csv.forEach(System.out::println);
        }
    }

    static InputStream readFromClasspath(final String name) throws FileNotFoundException {
        final var in = ExampleCsvReaderWithClasspathInput.class.getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException("Resource not found on classpath: " + name);
        }
        return in;
    }

}
