package example;

import java.io.StringWriter;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

/**
 * Example for writing CSV data with non-standard control characters.
 */
public class ExampleCsvWriterWithNonStandardControlCharacters {

    public static void main(final String[] args) {
        final StringWriter sw = new StringWriter();

        // The default configuration uses a comma as field separator,
        // a double quote as quote character and
        // a CRLF as line delimiter.
        CsvWriter.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .lineDelimiter(LineDelimiter.LF)
            .build(sw)
            .writeRecord("header1", "header2")
            .writeRecord("value1", "value;2");

        System.out.println(sw);
    }

}
