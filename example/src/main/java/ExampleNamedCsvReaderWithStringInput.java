import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with a header.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        header1,header2
        value1,value2
        foo,bar
        """;

    IO.println("Field 'header2' of each record:");
    CsvReader.builder().ofNamedCsvRecord(data)
        .forEach(rec -> IO.println(rec.getField("header2")));
}
