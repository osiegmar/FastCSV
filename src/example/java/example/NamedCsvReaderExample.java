package example;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

@SuppressWarnings("PMD.SystemPrintln")
public class NamedCsvReaderExample {

    public static void main(final String[] args) throws IOException {
        header();
        advancedConfiguration();
    }

    private static void header() {
        final Optional<NamedCsvRow> first = NamedCsvReader.builder()
            .build("header1,header2\nvalue1,value2")
            .stream().findFirst();

        first.ifPresent(row -> System.out.println("Header/Name based: " + row.getField("header2")));
    }

    private static void advancedConfiguration() {
        final String data = "'col a';'col b'\n'field 1';'field 2'";
        final String parsedData = NamedCsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .commentCharacter('#')
            .skipComments(false)
            .build(data)
            .stream()
            .map(csvRow -> csvRow.getFields().toString())
            .collect(Collectors.joining(" || "));

        System.out.println("Parsed via advanced config: " + parsedData);
    }

}
