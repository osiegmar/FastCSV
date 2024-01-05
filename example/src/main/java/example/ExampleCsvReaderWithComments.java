package example;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;

/**
 * Example for reading CSV data with comments.
 */
public class ExampleCsvReaderWithComments {

    private static final String DATA = "#line1\nfoo,bar";

    public static void main(final String[] args) {
        System.out.println("Reading data with no special treatment for comments:");
        CsvReader.builder()
            .ofCsvRecord(DATA)
            .forEach(System.out::println);

        System.out.println("Reading data while skipping comments:");
        CsvReader.builder()
            .commentStrategy(CommentStrategy.SKIP)
            .ofCsvRecord(DATA)
            .forEach(System.out::println);

        System.out.println("Reading data while reading comments:");
        CsvReader.builder()
            .commentStrategy(CommentStrategy.READ)
            .ofCsvRecord(DATA)
            .forEach(rec -> {
                if (rec.isComment()) {
                    System.out.println("Comment: " + rec.getField(0));
                } else {
                    System.out.println("Record: " + rec);
                }
            });
    }

}
