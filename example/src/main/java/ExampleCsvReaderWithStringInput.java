import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/// Example for reading CSV data from a String.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        foo,bar
        foo2,bar2
        """;

    IO.println("Reading data via for-each loop:");
    for (final CsvRecord csvRecord : CsvReader.builder().ofCsvRecord(data)) {
        IO.println(csvRecord.getFields());
    }

    IO.println("Reading data via forEach lambda:");
    CsvReader.builder().ofCsvRecord(data)
        .forEach(IO::println);

    IO.println("Reading data via stream:");
    CsvReader.builder().ofCsvRecord(data).stream()
        .map(rec -> rec.getField(1))
        .forEach(IO::println);

    IO.println("Reading data via iterator:");
    for (final var it = CsvReader.builder().ofCsvRecord(data).iterator(); it.hasNext(); ) {
        final CsvRecord csvRecord = it.next();
        IO.println(csvRecord.getFields());
    }
}
