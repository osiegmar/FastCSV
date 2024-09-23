package example;

import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for reading CSV data with a header.
 */
public class ExampleNamedCsvReaderWithStringInput {

    private static final String DATA = """
        header1,header2
        value1,value2
        foo,bar
        """;

    public static void main(final String[] args) {
        System.out.println("Field 'header2' of each record:");
        CsvReader.builder().ofNamedCsvRecord(DATA)
            .forEach(rec -> System.out.println(rec.getField("header2")));
    }

}
