package example;

import java.util.List;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;

/**
 * Example for reading CSV data with a custom header.
 */
public class ExampleNamedCsvReaderWithCustomHeader {

    private static final String DATA = "value1,value2\nfoo,bar";

    public static void main(final String[] args) {
        final List<String> header = List.of("header1", "header2");

        final CsvReader csvReader = CsvReader.builder().build(DATA);

        NamedCsvReader.from(csvReader, header)
            .forEach(rec -> System.out.println(rec.getField("header2")));
    }

}
