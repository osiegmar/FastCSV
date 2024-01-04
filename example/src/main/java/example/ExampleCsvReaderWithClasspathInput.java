package example;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/**
 * Example for reading CSV data from a file in the classpath.
 */
public class ExampleCsvReaderWithClasspathInput {

    public static void main(final String[] args) throws IOException {
        try (CsvReader<CsvRecord> csv = CsvReader.builder().build(getReader("/example.csv"))) {
            for (final CsvRecord csvRecord : csv) {
                System.out.println(csvRecord.getFields());
            }
        }
    }

    private static Reader getReader(final String name) {
        final InputStream in = ExampleCsvReaderWithClasspathInput.class.getResourceAsStream(name);
        if (in == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

}
