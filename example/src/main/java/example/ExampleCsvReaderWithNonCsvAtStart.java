package example;

import java.io.IOException;
import java.util.function.Predicate;

import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with non-CSV data before the actual CSV header.
public class ExampleCsvReaderWithNonCsvAtStart {

    private static final String DATA = """
        Your CSV file contains some non-CSV data before the actual CSV header?
        And you don't want to (mis)interpret them as CSV header? No problem!

        header1,header2
        foo,bar
        """;

    public static void main(final String[] args) throws IOException {
        alternative1();
        alternative2();
    }

    private static void alternative1() throws IOException {
        System.out.println("Alternative 1 - ignore specific number of lines");

        try (var csv = CsvReader.builder().ofNamedCsvRecord(DATA)) {
            // Skip the first 3 lines
            System.out.println("Skipping the first 3 lines");
            csv.skipLines(3);

            // Read the CSV data
            csv.forEach(System.out::println);
        }
    }

    private static void alternative2() throws IOException {
        System.out.println("Alternative 2 - wait for a specific line");

        final Predicate<String> isHeader = line ->
            line.contains("header1");

        try (var csv = CsvReader.builder().ofNamedCsvRecord(DATA)) {
            // Skip until the header line is found, but not more than 10 lines
            final int actualSkipped = csv.skipLines(isHeader, 10);
            System.out.println("Found header line after skipping " + actualSkipped + " lines");

            // Read the CSV data
            csv.forEach(System.out::println);
        }
    }

}
