import java.util.UUID;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

/// Example of interpreting empty, unquoted fields in a CSV as `null` values.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        firstName,middleName,lastName
        John,,Doe
        Jane,"",Smith
        """;

    final NamedCsvRecordHandler callbackHandler = NamedCsvRecordHandler
        .of(builder -> builder.fieldModifier(new NullFieldModifier()));

    CsvReader.builder().build(callbackHandler, data).stream()
        .map(this::mapPerson)
        .forEach(IO::println);
}

Person mapPerson(final NamedCsvRecord record) {
    return new Person(
        nullable(record.getField("firstName")),
        nullable(record.getField("middleName")),
        nullable(record.getField("lastName"))
    );
}

String nullable(final String fieldValue) {
    return NullFieldModifier.isNull(fieldValue) ? null : fieldValue;
}

static class NullFieldModifier implements FieldModifier {

    /// A marker to represent null values in the CSV. The unique UUID
    /// ensures that it does not conflict with any actual data in the CSV.
    private static final String NULL_MARKER = "NULL:" + UUID.randomUUID();

    /// {@return `NULL_MARKER` for unquoted empty fields,
    /// otherwise the field value itself}
    @Override
    public String modify(final long startingLineNumber, final int fieldIdx,
                         final boolean quoted, final String field) {
        return !quoted && field.isEmpty() ? NULL_MARKER : field;
    }

    /// {@return `true` if the field is the `NULL_MARKER`, otherwise `false`}
    static boolean isNull(final String field) {
        return NULL_MARKER.equals(field);
    }

}

record Person(String firstName, String middleName, String lastName) {
}
