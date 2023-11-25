package example;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

@SuppressWarnings({"PMD.SystemPrintln", "PMD.CloseResource"})
public class NamedCsvReaderExample {

    public static void main(final String[] args) {
        header();
        customHeader();
        customFieldModifier();
    }

    private static void header() {
        final Optional<NamedCsvRecord> first = NamedCsvReader.from(CsvReader.builder()
            .build("header1,header2\nvalue1,value2"))
            .stream().findFirst();

        first.ifPresent(csvRecord -> System.out.println("Header/Name based: " + csvRecord.getField("header2")));
    }

    private static void customHeader() {
        final List<String> header = List.of("header1", "header2");
        final CsvReader csvReader = CsvReader.builder()
            .build("value1,value2");
        final Optional<NamedCsvRecord> first = NamedCsvReader.from(csvReader, header)
            .stream().findFirst();

        first.ifPresent(csvRecord -> System.out.println("Header/Name based: " + csvRecord.getField("header2")));
    }

    private static void customFieldModifier() {
        System.out.print("Trim/Upper header via custom field modifier: ");
        final FieldModifier headerTrimUpperModifier = (originalLineNumber, fieldIdx, comment, quoted, field) ->
            originalLineNumber == 1 ? field.trim().toUpperCase(Locale.ROOT) : field;
        final CsvReader csvReader = CsvReader.builder()
            .fieldModifier(headerTrimUpperModifier)
            .build(" h1 , h2 \nfoo,bar");
        for (final NamedCsvRecord csvRecord : NamedCsvReader.from(csvReader)) {
            System.out.println(csvRecord.getFieldsAsMap());
        }
    }

}
