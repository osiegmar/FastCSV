package example;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

@SuppressWarnings("PMD.SystemPrintln")
public class NamedCsvReaderExample {

    public static void main(final String[] args) throws IOException {
        header();
        advancedConfiguration();
    }

    private static void header() {
        final Optional<NamedCsvRecord> first = NamedCsvReader.builder()
            .build("header1,header2\nvalue1,value2")
            .stream().findFirst();

        first.ifPresent(csvRecord -> System.out.println("Header/Name based: " + csvRecord.getField("header2")));
    }

    private static void advancedConfiguration() {
        final String data = "'col a';'col b'\n'field 1';'field 2'\n'field 3';'field 4'";
        final String parsedData = NamedCsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .commentCharacter('#')
            .skipComments(false)
            .build(data)
            .stream()
            .map(csvRecord -> csvRecord.getFieldsAsMap().toString())
            .collect(Collectors.joining(" || "));

        System.out.println("Parsed via advanced config: " + parsedData);
    }

}
