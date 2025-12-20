import java.io.IOException;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.StringArrayHandler;

/// Example for reading CSV with direct String array access.
///
/// String array access addresses the edge case where the number of allocated
/// objects needs to be minimized, e.g., in high-performance scenarios.
///
/// **ONLY use it if you are absolutely sure that you need it!**
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final String data = """
        jane,doe
        john,smith
        """;

    final var handler = StringArrayHandler.builder().build();

    try (CsvReader<String[]> csv = CsvReader.builder().build(handler, data)) {
        for (final String[] fields : csv) {
            IO.println("First Name: " + fields[0] + ", Last Name: " + fields[1]);
        }
    }
}
