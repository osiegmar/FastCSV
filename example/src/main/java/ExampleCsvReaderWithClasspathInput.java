import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/// Example for reading CSV data from a file in the classpath.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(readFromClasspath("/example.csv"))) {
        csv.forEach(IO::println);
    }
}

InputStream readFromClasspath(final String name) throws FileNotFoundException {
    final var in = getClass().getResourceAsStream(name);
    if (in == null) {
        throw new FileNotFoundException("Resource not found on classpath: " + name);
    }
    return in;
}
