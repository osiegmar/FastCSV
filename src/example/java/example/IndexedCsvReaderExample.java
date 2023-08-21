package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.siegmar.fastcsv.reader.CollectingStatusListener;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

@SuppressWarnings("PMD.SystemPrintln")
public class IndexedCsvReaderExample {

    public static void main(final String[] args) throws Exception {
        final int secondsToWrite = 3;
        final Path tmpFile = prepareTestFile(secondsToWrite);

        simple(tmpFile);
        statusMonitor(tmpFile);
        advancedConfiguration(tmpFile);
    }

    private static Path prepareTestFile(final long secondsToWrite) throws IOException {
        final Path tmpFile = createTmpFile();

        int row = 1;
        final long writeDuration = System.currentTimeMillis() + (secondsToWrite * 1000);

        try (CsvWriter csv = CsvWriter.builder().build(tmpFile)) {
            for (; System.currentTimeMillis() < writeDuration; row++) {
                csv.writeRow("row " + row, "containing standard ASCII, unicode letters öäü and emojis 😎");
            }
        }

        System.out.format("Temporary test file with %,d rows and %,d bytes successfully prepared%n%n",
            row - 1, Files.size(tmpFile));

        return tmpFile;
    }

    private static Path createTmpFile() throws IOException {
        final Path tmpFile = Files.createTempFile("FastCSV", ".csv");
        System.out.printf("# Prepare temporary test file %s%n", tmpFile);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (final IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        return tmpFile;
    }

    private static void simple(final Path file)
        throws IOException, ExecutionException, InterruptedException, TimeoutException {

        System.out.println("# Simple read");

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .pageSize(5)
            .build(file);

        try (csv) {
            // 1) As soon as the file has been indexed, the last page can be retrieved
            final CompletableFuture<List<CsvRow>> lastPage = csv.size()
                .thenCompose(pages -> {
                    System.out.format("Indexed %,d pages%n%n", pages);
                    return csv.readPage(pages - 1);
                });

            // 2) First rows are available right away
            System.out.println("Items of first page:");
            final List<CsvRow> firstPage = csv.readPage(0).get();
            firstPage.forEach(System.out::println);
            System.out.println();

            // Wait for 1) to complete
            final List<CsvRow> lastPageRows = lastPage.get(10, TimeUnit.SECONDS);
            System.out.println("Items of last page:");
            lastPageRows.forEach(System.out::println);
        }

        System.out.println();
    }

    private static void statusMonitor(final Path file) throws IOException {
        System.out.printf("# Read file with a total of %,d bytes%n", Files.size(file));

        final var statusListener = new CollectingStatusListener();

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .statusListener(statusListener)
            .build(file);

        try (csv) {
            // Indexing takes place in background – we can easily monitor the current status without blocking
            final var executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> System.out.println(statusListener),
                0, 250, TimeUnit.MILLISECONDS);

            final CompletableFuture<Integer> future = csv.size()
                .whenComplete((size, err) -> {
                    executor.shutdown();
                    if (err != null) {
                        err.printStackTrace(System.err);
                    } else {
                        System.out.printf("Finished reading file with a total of %,d rows%n%n", size);
                    }
                });

            // Wait for the completion of the future
            future.join();
        }
    }

    private static void advancedConfiguration(final Path file)
        throws IOException, ExecutionException, InterruptedException {

        final IndexedCsvReader csv = IndexedCsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .pageSize(5)
            .build(file);

        try (csv) {
            final List<CsvRow> rows = csv
                .readPage(2)
                .get();

            System.out.println("Parsed via advanced config:");
            rows.forEach(System.out::println);
        }
    }

}
