package example;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/// Example for reading CSV data from a String.
public class ExampleCsvReaderWithStringInput {

    private static final String DATA = """
        foo,bar
        foo2,bar2
        """;

    public static void main(final String[] args) {
        System.out.println("Reading data via for-each loop:");
        for (final CsvRecord csvRecord : CsvReader.builder().ofCsvRecord(DATA)) {
            System.out.println(csvRecord.getFields());
        }

        System.out.println("Reading data via forEach lambda:");
        CsvReader.builder().ofCsvRecord(DATA)
            .forEach(System.out::println);

        System.out.println("Reading data via stream:");
        CsvReader.builder().ofCsvRecord(DATA).stream()
            .map(rec -> rec.getField(1))
            .forEach(System.out::println);

        System.out.println("Reading data via iterator:");
        for (final var it = CsvReader.builder().ofCsvRecord(DATA).iterator(); it.hasNext();) {
            final CsvRecord csvRecord = it.next();
            System.out.println(csvRecord.getFields());
        }
    }

}
