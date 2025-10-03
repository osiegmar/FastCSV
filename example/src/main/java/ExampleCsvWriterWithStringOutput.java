import java.io.StringWriter;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data to a string.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final StringWriter sw = new StringWriter();
    CsvWriter.builder().build(sw)
        .writeRecord("header1", "header2")
        .writeRecord("value1", "value2");

    final String csv = sw.toString();
    IO.println(csv);
}
