package example;

import de.siegmar.fastcsv.reader.CsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvCallbackHandlers;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

/**
 * Example for reading CSV data with a custom header.
 */
public class ExampleNamedCsvReaderWithCustomHeader {

    private static final String DATA = "value1,value2\nfoo,bar";

    public static void main(final String[] args) {
        final CsvCallbackHandler<NamedCsvRecord> callbackHandler =
            CsvCallbackHandlers.ofNamedCsvRecord("header1", "header2");

        CsvReader.builder().build(callbackHandler, DATA)
            .forEach(rec -> System.out.println(rec.getField("header2")));
    }

}
