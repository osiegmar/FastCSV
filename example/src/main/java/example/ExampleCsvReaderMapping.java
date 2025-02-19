package example;

import java.io.IOException;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

/// Example for reading CSV data with a mapping function.
public class ExampleCsvReaderMapping {

    private static final String DATA = """
        ID,firstName,lastName
        1,John,Doe
        2,Jane,Smith
        """;

    public static void main(final String[] args) throws IOException {
        try (var persons = readPersons()) {
            persons.forEach(System.out::println);
        }
    }

    private static Stream<Person> readPersons() throws IOException {
        try (var csv = CsvReader.builder().ofNamedCsvRecord(DATA)) {
            return csv.stream().map(ExampleCsvReaderMapping::mapPerson);
        }
    }

    private static Person mapPerson(final NamedCsvRecord rec) {
        return new Person(
            Long.parseLong(rec.getField("ID")),
            rec.getField("firstName"),
            rec.getField("lastName")
        );
    }

    private record Person(long id, String firstName, String lastName) {
    }

}
