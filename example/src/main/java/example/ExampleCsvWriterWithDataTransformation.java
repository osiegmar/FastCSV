package example;

import java.io.IOException;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for transforming CSV data.
 */
public class ExampleCsvWriterWithDataTransformation {

    private static final String DATA = """
        firstname,initial,lastname,age
        john,h.,smith
        """;

    public static void main(final String[] args) throws IOException {
        try (CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(DATA);
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

}
