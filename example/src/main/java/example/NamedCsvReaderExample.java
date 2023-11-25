package example;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

@SuppressWarnings("PMD.SystemPrintln")
public class NamedCsvReaderExample {

    public static void main(final String[] args) {
        header();
        advancedConfiguration();
        customFieldModifier();
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

    private static void customFieldModifier() {
        System.out.print("Trim/Upper header via custom field modifier: ");
        final FieldModifier headerTrimUpperModifier = (originalLineNumber, fieldIdx, comment, quoted, field) ->
            originalLineNumber == 1 ? field.trim().toUpperCase(Locale.ROOT) : field;
        final var csvBuilder = NamedCsvReader.builder()
            .fieldModifier(headerTrimUpperModifier);
        for (final NamedCsvRecord csvRecord : csvBuilder.build(" h1 , h2 \nfoo,bar")) {
            System.out.println(csvRecord.getFieldsAsMap());
        }
    }

}
