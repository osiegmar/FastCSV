import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;

/// Example for reading CSV data with faulty (or ambiguous) data.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        foo,bar
        foo
        foo,bar,baz
        """;

    IO.println("Reading data with default settings:");
    try {
        CsvReader.builder()
            .ofCsvRecord(data)
            .forEach(IO::println);
    } catch (final CsvParseException e) {
        IO.println("Exception expected due to different field counts:");
        e.printStackTrace(System.out);
    }

    IO.println("Reading data while ignoring different field counts:");
    CsvReader.builder()
        .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
        .missingFieldStrategy(FieldMismatchStrategy.IGNORE)
        .ofCsvRecord(data)
        .forEach(IO::println);

    IO.println("Reading data while skipping records with different field counts:");
    CsvReader.builder()
        .extraFieldStrategy(FieldMismatchStrategy.SKIP)
        .missingFieldStrategy(FieldMismatchStrategy.SKIP)
        .ofCsvRecord(data)
        .forEach(IO::println);
}
