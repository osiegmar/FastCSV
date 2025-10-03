import java.io.IOException;
import java.util.function.Predicate;

import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with non-CSV data before the actual CSV header.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final String data = """
        Your CSV file contains some non-CSV data before the actual CSV header?
        And you don't want to (mis)interpret them as CSV header? No problem!
        
        header1,header2
        foo,bar
        """;

    alternative1(data);
    alternative2(data);
}

void alternative1(final String data) throws IOException {
    IO.println("Alternative 1 - ignore specific number of lines");

    try (var csv = CsvReader.builder().ofNamedCsvRecord(data)) {
        // Skip the first 3 lines
        IO.println("Skipping the first 3 lines");
        csv.skipLines(3);

        // Read the CSV data
        csv.forEach(IO::println);
    }
}

void alternative2(final String data) throws IOException {
    IO.println("Alternative 2 - wait for a specific line");

    final Predicate<String> isHeader = line ->
        line.contains("header1");

    try (var csv = CsvReader.builder().ofNamedCsvRecord(data)) {
        // Skip until the header line is found, but not more than 10 lines
        final int actualSkipped = csv.skipLines(isHeader, 10);
        IO.println("Found header line after skipping " + actualSkipped + " lines");

        // Read the CSV data
        csv.forEach(IO::println);
    }
}
