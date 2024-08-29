package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.siegmar.fastcsv.reader.CollectingStatusListener;
import de.siegmar.fastcsv.reader.CsvIndex;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for reading CSV data from a file using an index (non-streaming).
 */
public class ExampleIndexedCsvReader {

    public static void main(final String[] args) throws Exception {
        final Path tmpFile = prepareTestFile(Duration.ofSeconds(3));

        simple(tmpFile);
        reuseIndex(tmpFile);
        statusMonitor(tmpFile);
    }

    private static Path prepareTestFile(final Duration timeToWrite)
        throws IOException {

        final Path tmpFile = createTmpFile();

        int record = 1;
        final long writeUntil = System.currentTimeMillis()
            + timeToWrite.toMillis();

        try (CsvWriter csv = CsvWriter.builder().build(tmpFile)) {
            for (; System.currentTimeMillis() < writeUntil; record++) {
                csv.writeRecord("record " + record,
                    "containing ASCII, some umlauts öäü and an emoji 😎");
            }
        }

        System.out.format("Temporary test file with %,d records and "
                + "%,d bytes successfully prepared%n%n",
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

        final IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder()
            .pageSize(5)
            .ofCsvRecord(file);

        try (csv) {
            final CsvIndex index = csv.getIndex();
            System.out.printf("Indexed %,d records%n", index.getRecordCount());

            System.out.println("Show records of last page");
            final int lastPage = index.getPageCount() - 1;
            final List<CsvRecord> lastPageRecords = csv.readPage(lastPage);
            lastPageRecords.forEach(System.out::println);
        }

        System.out.println();
    }

    private static void reuseIndex(final Path file) throws IOException {
        System.out.println("# Reuse Index");

        final CsvIndex csvIndex = IndexedCsvReader.builder()
            .pageSize(5)
            .ofCsvRecord(file)
            .getIndex();

        System.out.printf("Indexed %,d records%n", csvIndex.getRecordCount());

        // Store index for the given file somewhere, and use it later ...

        final IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder()
            .pageSize(5)
            .index(csvIndex)
            .ofCsvRecord(file);

        try (csv) {
            System.out.println("Show records of last page");
            final int lastPage = csvIndex.getPageCount() - 1;
            final List<CsvRecord> lastPageRecords = csv.readPage(lastPage);
            lastPageRecords.forEach(System.out::println);
        }

        System.out.println();
    }

    private static void statusMonitor(final Path file) throws IOException {
        System.out.printf("# Read file with %,d bytes%n", Files.size(file));

        final var statusListener = new CollectingStatusListener();

        // Using the StatusListener, we can monitor the
        // indexing process in the background
        final var executor = Executors
            .newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(
            () -> {
                if (statusListener.isCompleted()) {
                    executor.shutdown();
                } else {
                    System.out.println(statusListener);
                }
            },
            0, 250, TimeUnit.MILLISECONDS);

        final IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder()
            .statusListener(statusListener)
            .ofCsvRecord(file);

        try (csv) {
            System.out.printf("Indexed %,d records%n",
                csv.getIndex().getRecordCount());
        }

        System.out.println();
    }

}
