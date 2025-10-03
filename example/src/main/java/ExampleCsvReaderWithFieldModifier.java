import java.util.Locale;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;

/// Example for reading CSV data from a String while using a field modifier.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = " foo , BAR \n FOO2 , bar2 ";

    IO.println("Trim fields:");
    CsvReader.builder()
        .build(CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM)), data)
        .forEach(IO::println);

    IO.println("Combine modifiers (trim/lowercase):");
    CsvReader.builder()
        .build(CsvRecordHandler.of(c -> c.fieldModifier(combinedModifier())), data)
        .forEach(IO::println);

    IO.println("Custom modifier (trim/lowercase on first record):");
    CsvReader.builder()
        .build(CsvRecordHandler.of(c -> c.fieldModifier(new CustomModifier())), data)
        .forEach(IO::println);
}

FieldModifier combinedModifier() {
    final FieldModifier toLower =
        FieldModifier.modify(field -> field.toLowerCase(Locale.ENGLISH));
    return FieldModifiers.TRIM.andThen(toLower);
}

static final class CustomModifier implements FieldModifier {

    @Override
    public String modify(final long startingLineNumber, final int fieldIdx,
                         final boolean quoted, final String field) {
        if (startingLineNumber == 1) {
            return field.trim().toLowerCase(Locale.ENGLISH);
        }

        return field;
    }

}
