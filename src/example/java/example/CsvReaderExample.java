package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;

@SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidFileStream"})
public class CsvReaderExample {

    public static void main(final String[] args) {
        simple();
        forEachLambda();
        stream();
        iterator();
        header();
        advancedConfiguration();
        file();
        randomAccessFile();
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
            .map(csvRow -> csvRow.getFields().toString())
            .collect(Collectors.joining(" || "));

        System.out.println("Parsed via advanced config: " + parsedData);
    }

    private static void file() {
        try {
            final Path path = Files.createTempFile("fastcsv", ".csv");
            Files.write(path, Collections.singletonList("foo,bar\n"));

            try (CsvReader csvReader = CsvReader.builder().build(path)) {
                csvReader.forEach(System.out::println);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void randomAccessFile() {
        try {
            final Path path = Files.createTempFile("fastcsv", ".csv");
            try (CsvWriter writer = CsvWriter.builder().build(path, UTF_8)) {
                for (int row = 0; row < 100; row++) {
                    writer.writeRow("row " + row, "foo", "bar");
                }
            }

            // collect row offsets (could also be done in larger chunks)
            final List<Long> offsets;
            try (CsvReader csvReader = CsvReader.builder().build(path, UTF_8)) {
                offsets = csvReader.stream()
                    .map(CsvRow::getStartingOffset)
                    .collect(Collectors.toList());
            }

            // random access read with offset seeking
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
                 FileInputStream fin = new FileInputStream(raf.getFD());
                 InputStreamReader isr = new InputStreamReader(fin, UTF_8);
                 CsvReader reader = CsvReader.builder().build(isr);
                 CloseableIterator<CsvRow> iterator = reader.iterator()) {

                // seek to file offset of row 10
                raf.seek(offsets.get(10));
                reader.resetBuffer();
                System.out.println(iterator.next());

                // seek to file offset of row 50
                raf.seek(offsets.get(50));
                reader.resetBuffer();
                System.out.println(iterator.next());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
