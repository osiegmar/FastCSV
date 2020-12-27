package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

@SuppressWarnings("PMD.SystemPrintln")
public class CsvReaderExample {

    public static void main(final String[] args) {
        simple();
        forEachLambda();
        stream();
        iterator();
        header();
        advancedConfiguration();
        file();
    }

    private static void simple() {
        System.out.print("For-Each loop: ");
        for (final CsvRow csvRow : CsvReader.builder().build("foo,bar")) {
            System.out.println(Arrays.toString(csvRow.getFields()));
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
            System.out.print(Arrays.toString(csvRow.getFields()));
            if (iterator.hasNext()) {
                System.out.print(" || ");
            } else {
                System.out.println();
            }
        }
    }

    private static void header() {
        final Optional<NamedCsvRow> first = NamedCsvReader.builder()
            .build("header1,header2\nvalue1,value2")
            .stream().findFirst();

        first.ifPresent(row -> System.out.println("Header/Name based: " + row.getField("header2")));
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
            .map(csvRow -> Arrays.toString(csvRow.getFields()))
            .collect(Collectors.joining(" || "));

        System.out.println("Parsed via advanced config: " + parsedData);
    }

    private static void file() {
        try {
            final Path path = Files.createTempFile("fastcsv", ".csv");
            Files.write(path, "foo,bar\n".getBytes(UTF_8));

            try (CsvReader csvReader = CsvReader.builder().build(path, UTF_8)) {
                csvReader.forEach(System.out::println);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
