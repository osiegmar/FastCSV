import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with comments.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        #line1
        foo,bar
        """;

    IO.println("Reading data with no special treatment for comments:");
    CsvReader.builder()
        .ofCsvRecord(data)
        .forEach(IO::println);

    IO.println("Reading data while skipping comments:");
    CsvReader.builder()
        .commentStrategy(CommentStrategy.SKIP)
        .ofCsvRecord(data)
        .forEach(IO::println);

    IO.println("Reading data while reading comments:");
    CsvReader.builder()
        .commentStrategy(CommentStrategy.READ)
        .ofCsvRecord(data)
        .forEach(rec -> {
            if (rec.isComment()) {
                IO.println("Comment: " + rec.getField(0));
            } else {
                IO.println("Record: " + rec);
            }
        });
}
