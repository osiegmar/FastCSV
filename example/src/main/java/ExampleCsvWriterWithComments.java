import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data with comments (not part of the CSV standard).
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    CsvWriter.builder().toConsole()
        .writeComment("A comment can be placed\nanywhere")
        .writeRecord("field 1", "field 2", "field 3\n#with a line break")
        .writeComment("in the CSV file");
}
