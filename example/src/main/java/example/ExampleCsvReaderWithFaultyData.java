package example;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for reading CSV data with faulty (or ambiguous) data.
 */
public class ExampleCsvReaderWithFaultyData {

    private static final String DATA = "foo,bar\nonly one field followed by some empty lines\n\n\nbar,foo";

    public static void main(final String[] args) {
        System.out.println("Reading data with lenient (default) settings:");
        CsvReader.builder()
            .build(DATA)
            .forEach(System.out::println);

        System.out.println("Reading data while not skipping empty lines:");
        CsvReader.builder()
            .skipEmptyLines(false)
            .build(DATA)
            .forEach(System.out::println);

        try {
            CsvReader.builder()
                .ignoreDifferentFieldCount(false)
                .build(DATA)
                .forEach(System.out::println);
        } catch (final CsvParseException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause().getMessage());
        }
    }

}
