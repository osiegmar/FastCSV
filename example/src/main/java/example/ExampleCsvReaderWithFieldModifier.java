package example;

import java.util.Locale;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;

/// Example for reading CSV data from a String while using a field modifier.
public class ExampleCsvReaderWithFieldModifier {

    private static final String DATA = " foo , BAR \n FOO2 , bar2 ";

    public static void main(final String[] args) {
        System.out.println("Trim fields:");
        CsvReader.builder()
            .build(CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM)), DATA)
            .forEach(System.out::println);

        System.out.println("Combine modifiers (trim/lowercase):");
        CsvReader.builder()
            .build(CsvRecordHandler.of(c -> c.fieldModifier(combinedModifier())), DATA)
            .forEach(System.out::println);

        System.out.println("Custom modifier (trim/lowercase on first record):");
        CsvReader.builder()
            .build(CsvRecordHandler.of(c -> c.fieldModifier(new CustomModifier())), DATA)
            .forEach(System.out::println);
    }

    private static FieldModifier combinedModifier() {
        final FieldModifier toLower =
            FieldModifier.modify(field -> field.toLowerCase(Locale.ENGLISH));
        return FieldModifiers.TRIM.andThen(toLower);
    }

    private static class CustomModifier implements FieldModifier {

        @Override
        public String modify(final long startingLineNumber, final int fieldIdx,
                             final boolean quoted, final String field) {
            if (startingLineNumber == 1) {
                return field.trim().toLowerCase(Locale.ENGLISH);
            }

            return field;
        }

    }

}
