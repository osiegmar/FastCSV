import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data field by field.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    CsvWriter.builder().toConsole()
        .writeRecord("header1", "header2")
        .writeRecord().writeField("value1").writeField("value2").endRecord();
}
