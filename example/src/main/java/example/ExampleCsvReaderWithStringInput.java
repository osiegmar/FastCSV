package example;

import java.util.Iterator;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/**
 * Example for reading CSV data from a String.
 */
public class ExampleCsvReaderWithStringInput {

    private static final String DATA = "foo,bar\nfoo2,bar2";

    public static void main(final String[] args) {
        System.out.println("Reading data via for-each loop:");
        for (final CsvRecord csvRecord : CsvReader.builder().build(DATA)) {
            System.out.println(csvRecord.getFields());
        }

        System.out.println("Reading data via forEach lambda:");
        CsvReader.builder().build(DATA)
            .forEach(System.out::println);

        System.out.println("Reading data via stream:");
        CsvReader.builder().build(DATA).stream()
            .map(rec -> rec.getField(1))
            .forEach(System.out::println);

        System.out.println("Reading data via iterator:");
        for (final Iterator<CsvRecord> iterator = CsvReader.builder().build(DATA).iterator(); iterator.hasNext();) {
            final CsvRecord csvRecord = iterator.next();
            System.out.println(csvRecord.getFields());
        }
    }

}
