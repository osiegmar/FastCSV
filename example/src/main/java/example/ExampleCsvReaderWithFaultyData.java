package example;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with faulty (or ambiguous) data.
public class ExampleCsvReaderWithFaultyData {

    private static final String DATA = """
        foo,bar
        foo
        foo,bar,baz
        """;

    public static void main(final String[] args) {
        System.out.println("Reading data with default settings:");
        try {
            CsvReader.builder()
                .ofCsvRecord(DATA)
                .forEach(System.out::println);
        } catch (final CsvParseException e) {
            System.out.println("Exception expected due to different field counts:");
            e.printStackTrace(System.out);
        }

        System.out.println("Reading data while not ignoring different field counts:");
        CsvReader.builder()
            .allowExtraFields(true)
            .allowMissingFields(true)
            .ofCsvRecord(DATA)
            .forEach(System.out::println);
    }

}
