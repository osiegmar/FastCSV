import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.HeaderValidator;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

/// Example for reading CSV data while validating the header.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        header1,header2
        foo,bar
        """;

    IO.println("Require an exact header (order-sensitive):");
    final NamedCsvRecordHandler exactHandler = NamedCsvRecordHandler.of(builder ->
        builder.headerValidator(HeaderValidator.containsExactly("header1", "header2"))
    );
    CsvReader.builder().build(exactHandler, data)
        .forEach(rec -> IO.println(rec.getField("header2")));

    IO.println("Require certain fields (in any order; additional fields are allowed):");
    final NamedCsvRecordHandler atLeastHandler = NamedCsvRecordHandler.of(builder ->
        builder.headerValidator(HeaderValidator.containsAtLeast("header2"))
    );
    CsvReader.builder().build(atLeastHandler, data)
        .forEach(rec -> IO.println(rec.getField("header2")));

    IO.println("Custom validation logic:");
    final NamedCsvRecordHandler customHandler = NamedCsvRecordHandler.of(builder ->
        builder.headerValidator(header -> {
            if (header.size() != 2) {
                throw new CsvParseException("Expected 2 header fields, but found " + header.size());
            }
        })
    );
    CsvReader.builder().build(customHandler, data)
        .forEach(rec -> IO.println(rec.getField("header2")));

    IO.println("A failed validation aborts reading with a CsvParseException:");
    final NamedCsvRecordHandler failingHandler = NamedCsvRecordHandler.of(builder ->
        builder.headerValidator(HeaderValidator.containsExactly("headerA", "headerB"))
    );
    try {
        CsvReader.builder().build(failingHandler, data)
            .forEach(rec -> IO.println(rec.getField("header2")));
    } catch (final CsvParseException e) {
        IO.println(e.getMessage());
    }
}
