package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

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
        for (final CsvRecord csvRecord : CsvReader.builder().build("foo,bar")) {
            System.out.println(csvRecord.getFields());
        }
    }

    private static void forEachLambda() {
        System.out.print("Loop using forEach lambda: ");
        CsvReader.builder().build("foo,bar")
            .forEach(System.out::println);
    }

    private static void stream() {
        System.out.printf("CSV contains %d records%n",
            CsvReader.builder().build("foo,bar").stream().count());
    }

    private static void iterator() {
        System.out.print("Iterator loop: ");
        for (final Iterator<CsvRecord> iterator = CsvReader.builder()
            .build("foo,bar\nfoo2,bar2").iterator(); iterator.hasNext();) {

            final CsvRecord csvRecord = iterator.next();
            System.out.print(csvRecord.getFields());
            if (iterator.hasNext()) {
                System.out.print(" || ");
            } else {
                System.out.println();
            }
        }
    }

    private static void advancedConfiguration() {
        final String data = "#commented record\n'quoted ; column';second column\nnew record";
        final String parsedData = CsvReader.builder()
            .fieldSeparator(';')
            .quoteCharacter('\'')
            .commentStrategy(CommentStrategy.SKIP)
            .commentCharacter('#')
            .skipEmptyRecords(true)
            .errorOnDifferentFieldCount(false)
            .build(data)
            .stream()
            .map(csvRecord -> csvRecord.getFields().toString())
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
