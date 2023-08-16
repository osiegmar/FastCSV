package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

@SuppressWarnings("PMD.SystemPrintln")
public class CsvReaderExample {

    public static void main(final String[] args) throws IOException {
        simple();
        forEachLambda();
        stream();
        iterator();
        advancedConfiguration();
        file();
    }

    private static void simple() {
        System.out.print("For-Each loop: ");
        for (final CsvRow csvRow : CsvReader.builder().build("foo,bar")) {
            System.out.println(csvRow.getFields());
        }
    }

    private static void forEachLambda() {
        System.out.print("Loop using forEach lambda: ");
        CsvReader.builder().build("foo,bar")
            .forEach(System.out::println);
    }

    private static void stream() {
        System.out.printf("CSV contains %d rows%n",
            CsvReader.builder().build("foo,bar").stream().count());
    }

    private static void iterator() {
        System.out.print("Iterator loop: ");
        for (final Iterator<CsvRow> iterator = CsvReader.builder()
            .build("foo,bar\nfoo2,bar2").iterator(); iterator.hasNext();) {

            final CsvRow csvRow = iterator.next();
            System.out.print(csvRow.getFields());
            if (iterator.hasNext()) {
                System.out.print(" || ");
            } else {
                System.out.println();
            }
        }
    }

    private static void advancedConfiguration() {
        final String data = "#commented row\n'quoted ; column';second column\nnew row";
        final String parsedData = CsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .commentStrategy(CommentStrategy.SKIP)
            .commentCharacter('#')
            .skipEmptyRows(true)
            .errorOnDifferentFieldCount(false)
            .build(data)
            .stream()
            .map(csvRow -> csvRow.getFields().toString())
            .collect(Collectors.joining(" || "));

        System.out.println("Parsed via advanced config: " + parsedData);
    }

    private static void file() throws IOException {
        final Path path = Files.createTempFile("fastcsv", ".csv");
        Files.writeString(path, "foo,bar\n");

        try (CsvReader csv = CsvReader.builder().build(path)) {
            csv.forEach(System.out::println);
        }
    }

}
