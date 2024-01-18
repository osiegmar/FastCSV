package example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

/**
 * Example for reading CSV data with non-CSV data before the actual CSV header.
 */
public class ExampleCsvReaderWithNonCsvAtStart {

    private static final String DATA = """
        Your CSV file contains some non-CSV data before the actual CSV header?
        And you don't want to misinterpret them as CSV header? No problem!

        header1,header2
        foo,bar
        """;

    public static void main(final String[] args) throws IOException {
        alternative1();
        alternative2();
    }

    private static void alternative1() throws IOException {
        System.out.println("Alternative 1 - ignore specific number of lines");
        final CsvReader.CsvReaderBuilder builder = CsvReader.builder().ignoreDifferentFieldCount(false);

        try (var br = new BufferedReader(new StringReader(DATA))) {
            // ignore the first 3 lines
            br.lines().limit(3).forEach(r -> { });

            builder.ofNamedCsvRecord(br)
                .forEach(System.out::println);
        }
    }

    private static void alternative2() throws IOException {
        System.out.println("Alternative 2 - wait for a specific line");
        final CsvReader.CsvReaderBuilder builder = CsvReader.builder().ignoreDifferentFieldCount(false);

        try (var br = new BufferedReader(new StringReader(DATA))) {
            // Look for the CSV header but read at most 100 lines
            final List<String> header = br.lines()
                .limit(100)
                .filter(l -> l.contains("header1,header2"))
                .findFirst()
                .map(line -> builder.ofCsvRecord(line).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Illegal header: " + line))
                    .getFields())
                .orElseThrow(() -> new IllegalStateException("No CSV header found"));

            builder.build(new NamedCsvRecordHandler(header), br)
                .forEach(System.out::println);
        }
    }

}
