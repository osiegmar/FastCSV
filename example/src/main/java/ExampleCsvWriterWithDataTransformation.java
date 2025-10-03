import java.io.IOException;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for transforming CSV data.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final String data = """
        firstname,initial,lastname,age
        john,h.,smith
        """;

    try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(data);
         CsvWriter writer = CsvWriter.builder().toConsole()) {

        // transform firstname, initial, lastname to lastname, firstname
        writer.writeRecord("lastname", "firstname");
        for (final NamedCsvRecord csvRecord : reader) {
            final String lastname = csvRecord.getField("lastname");
            final String firstname = csvRecord.getField("firstname");
            writer.writeRecord(lastname, firstname);
        }
    }
}
