import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with control characters that are
/// not the default/standard ones.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        'foo';'bar'
        'foo2';'bar2'
        """;

    CsvReader.builder()
        .fieldSeparator(';')
        .quoteCharacter('\'')
        .ofCsvRecord(data)
        .forEach(IO::println);
}
