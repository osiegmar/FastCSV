package example;

import java.io.StringWriter;

import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for writing CSV data field by field.
 */
public class ExampleCsvWriterWithSingleFields {

    public static void main(final String[] args) {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw)
            .writeRecord("header1", "header2")
            .writeRecord().writeField("value1").writeField("value2").endRecord();

        System.out.println(sw);
    }

}
