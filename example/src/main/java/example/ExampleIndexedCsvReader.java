package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.siegmar.fastcsv.reader.CollectingStatusListener;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvIndex;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for reading CSV data from a file using an index (non-streaming).
 */
public class ExampleIndexedCsvReader {

    public static void main(final String[] args) throws Exception {
        final int secondsToWrite = 3;
        final Path tmpFile = prepareTestFile(secondsToWrite);

        simple(tmpFile);
        reuseIndex(tmpFile);
        statusMonitor(tmpFile);
        advancedConfiguration(tmpFile);
    }

    private static Path prepareTestFile(final long secondsToWrite) throws IOException {
        final Path tmpFile = createTmpFile();

        int record = 1;
        final long writeDuration = System.currentTimeMillis() + (secondsToWrite * 1000);

        try (CsvWriter csv = CsvWriter.builder().build(tmpFile)) {
            for (; System.currentTimeMillis() < writeDuration; record++) {
                csv.writeRecord("record " + record, "containing standard ASCII, unicode letters öäü and emojis 😎");
            }
        }

        System.out.format("Temporary test file with %,d records and %,d bytes successfully prepared%n%n",
            record - 1, Files.size(tmpFile));

        return tmpFile;
    }

    private static Path createTmpFile() throws IOException {
        final Path tmpFile = Files.createTempFile("FastCSV", ".csv");
        tmpFile.toFile().deleteOnExit();
        System.out.printf("# Prepare temporary test file %s%n", tmpFile);
        return tmpFile;
    }

    private static void simple(final Path file) throws IOException {
        System.out.println("# Simple read");

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .pageSize(5)
            .build(file);

        try (csv) {
            final CsvIndex index = csv.index();
            System.out.printf("Indexed %,d records%n", index.recordCount());

            System.out.println("Show records of last page");
            final int lastPage = index.pageCount() - 1;
            final List<CsvRecord> lastPageRecords = csv.readPage(lastPage);
            lastPageRecords.forEach(System.out::println);
        }

        System.out.println();
    }

    private static void reuseIndex(final Path file) throws IOException {
        System.out.println("# Reuse Index");

        final CsvIndex csvIndex = IndexedCsvReader.builder()
            .pageSize(5)
            .build(file)
            .index();

        System.out.printf("Indexed %,d records%n", csvIndex.recordCount());

        // Store index for the given file somewhere, and use it later ...

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .pageSize(5)
            .index(csvIndex)
            .build(file);

        try (csv) {
            System.out.println("Show records of last page");
            final int lastPage = csvIndex.pageCount() - 1;
            final List<CsvRecord> lastPageRecords = csv.readPage(lastPage);
            lastPageRecords.forEach(System.out::println);
        }

        System.out.println();
    }

    private static void statusMonitor(final Path file) throws IOException {
        System.out.printf("# Read file with a total of %,d bytes%n", Files.size(file));

        final var statusListener = new CollectingStatusListener();

        // Using the StatusListener we can monitor the indexing process in background
        final var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(
            () -> {
                if (statusListener.isCompleted()) {
                    executor.shutdown();
                } else {
                    System.out.println(statusListener);
                }
            },
            0, 250, TimeUnit.MILLISECONDS);

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .statusListener(statusListener)
            .build(file);

        try (csv) {
            System.out.printf("Indexed %,d records%n", csv.index().recordCount());
        }

        System.out.println();
    }

    private static void advancedConfiguration(final Path file) throws IOException {
        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .pageSize(5)
            .build(file);

        try (csv) {
            final List<CsvRecord> csvRecords = csv.readPage(2);

            System.out.println("Parsed via advanced config:");
            csvRecords.forEach(System.out::println);
        }
    }

}
