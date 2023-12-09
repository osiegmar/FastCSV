package example;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;

/**
 * Example for reading CSV data with a header.
 */
public class ExampleNamedCsvReaderWithStringInput {

    private static final String DATA = "header1,header2\nvalue1,value2\nfoo,bar";

    public static void main(final String[] args) {
        System.out.println("Field 'header2' of each record:");
        NamedCsvReader.from(CsvReader.builder().build(DATA))
            .forEach(rec -> System.out.println(rec.getField("header2")));
    }

}
