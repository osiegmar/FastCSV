package example;

import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for reading CSV data with control characters that are not the default/standard ones.
 */
public class ExampleCsvReaderWithNonStandardControlCharacters {

    private static final String DATA = "'foo';'bar'\n'foo2';'bar2'";

    public static void main(final String[] args) {
        CsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .ofCsvRecord(DATA)
            .forEach(System.out::println);
    }

}
