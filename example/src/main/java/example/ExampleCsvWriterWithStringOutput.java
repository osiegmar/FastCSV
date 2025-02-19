package example;

import java.io.StringWriter;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data to a string.
public class ExampleCsvWriterWithStringOutput {

    public static void main(final String[] args) {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw)
            .writeRecord("header1", "header2")
            .writeRecord("value1", "value2");

        final String csv = sw.toString();
        System.out.println(csv);
    }

}
