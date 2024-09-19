package example;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

/**
 * Example for writing CSV data with non-standard control characters.
 */
public class ExampleCsvWriterWithNonStandardControlCharacters {

    public static void main(final String[] args) {
        // The default configuration uses a comma as field separator,
        // a double quote as quote character and
        // a CRLF as line delimiter.
        CsvWriter.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .lineDelimiter(LineDelimiter.LF)
            .toConsole()
            .writeRecord("header1", "header2")
            .writeRecord("value1", "value;2");
    }

}
