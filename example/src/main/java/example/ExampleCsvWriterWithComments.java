package example;

import java.io.IOException;
import java.io.StringWriter;

import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for writing CSV data with comments (not part of the CSV standard).
 */
public class ExampleCsvWriterWithComments {

    public static void main(final String[] args) throws IOException {
        final StringWriter sw = new StringWriter();
        CsvWriter.builder().build(sw)
            .writeComment("A comment can be placed")
            .writeRecord("header1", "header2")
            .writeComment("anywhere")
            .writeRecord("value1", "value2")
            .writeComment("in the CSV file");

        System.out.println(sw);
    }

}
