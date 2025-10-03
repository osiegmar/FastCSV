import java.io.IOException;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

/// Example for reading CSV data with a mapping function.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    try (var persons = readPersons()) {
        persons.forEach(IO::println);
    }
}

Stream<Person> readPersons() throws IOException {
    final String data = """
        ID,firstName,lastName
        1,John,Doe
        2,Jane,Smith
        """;

    try (var csv = CsvReader.builder().ofNamedCsvRecord(data)) {
        return csv.stream().map(this::mapPerson);
    }
}

Person mapPerson(final NamedCsvRecord rec) {
    return new Person(
        Long.parseLong(rec.getField("ID")),
        rec.getField("firstName"),
        rec.getField("lastName")
    );
}

record Person(long id, String firstName, String lastName) {
}
