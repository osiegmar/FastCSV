package example;

import java.util.Locale;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;

/**
 * Example for reading CSV data from a String while using a field modifier.
 */
public class ExampleCsvReaderWithFieldModifier {

    private static final String DATA = " foo , BAR \n FOO2 , bar2 ";

    public static void main(final String[] args) {
        System.out.println("Trim fields:");
        builderWithTrim().ofCsvRecord(DATA)
            .forEach(System.out::println);

        System.out.println("Trim and lowercase fields:");
        builderWithTrimAndLowerCase().ofCsvRecord(DATA)
            .forEach(System.out::println);

        System.out.println("Trim and lowercase fields of first record (by using a custom modifier):");
        builderWithCustomModifier().ofCsvRecord(DATA)
            .forEach(System.out::println);
    }

    private static CsvReader.CsvReaderBuilder builderWithTrim() {
        return CsvReader.builder().fieldModifier(FieldModifiers.TRIM);
    }

    private static CsvReader.CsvReaderBuilder builderWithTrimAndLowerCase() {
        final FieldModifier modifier = FieldModifiers.TRIM.andThen(FieldModifiers.lower(Locale.ENGLISH));
        return CsvReader.builder().fieldModifier(modifier);
    }

    private static CsvReader.CsvReaderBuilder builderWithCustomModifier() {
        final FieldModifier modifier = new FieldModifier() {
            @Override
            public String modify(final long startingLineNumber, final int fieldIdx, final boolean quoted,
                                 final String field) {
                if (startingLineNumber == 1) {
                    return field.trim().toLowerCase(Locale.ENGLISH);
                }
                return field;
            }
        };

        return CsvReader.builder().fieldModifier(modifier);
    }

}
