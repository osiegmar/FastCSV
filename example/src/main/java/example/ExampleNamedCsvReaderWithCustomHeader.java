package example;

import de.siegmar.fastcsv.reader.CsvCallbackHandlers;
import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for reading CSV data with a custom header.
 */
public class ExampleNamedCsvReaderWithCustomHeader {

    private static final String DATA = "value1,value2\nfoo,bar";

    public static void main(final String[] args) {
        CsvReader.builder().build(DATA, CsvCallbackHandlers.ofNamedCsvRecord("header1", "header2"))
            .forEach(rec -> System.out.println(rec.getField("header2")));
    }

}
