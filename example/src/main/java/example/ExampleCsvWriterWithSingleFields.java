package example;

import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for writing CSV data field by field.
 */
public class ExampleCsvWriterWithSingleFields {

    public static void main(final String[] args) {
        CsvWriter.builder().toConsole()
            .writeRecord("header1", "header2")
            .writeRecord().writeField("value1").writeField("value2").endRecord();
    }

}
