import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

/// Example for reading CSV data with a custom header.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        value1,value2
        foo,bar
        """;

    final NamedCsvRecordHandler callbackHandler = NamedCsvRecordHandler.of(builder ->
        builder.header("header1", "header2")
    );

    CsvReader.builder().build(callbackHandler, data)
        .forEach(rec -> IO.println(rec.getField("header2")));
}
