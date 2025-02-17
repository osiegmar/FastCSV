package example;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data with comments (not part of the CSV standard).
public class ExampleCsvWriterWithComments {

    public static void main(final String[] args) {
        CsvWriter.builder().toConsole()
            .writeComment("A comment can be placed\nanywhere")
            .writeRecord("field 1", "field 2", "field 3\n#with a line break")
            .writeComment("in the CSV file");
    }

}
