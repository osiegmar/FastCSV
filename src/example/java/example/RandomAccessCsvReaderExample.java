package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CountingStatusListener;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.RandomAccessCsvReader;
import de.siegmar.fastcsv.reader.StatusListener;
import de.siegmar.fastcsv.writer.CsvWriter;

@SuppressWarnings("PMD.SystemPrintln")
public class RandomAccessCsvReaderExample {

    public static void main(final String[] args) throws Exception {
        final int secondsToWrite = 3;
        final Path tmpFile = prepareTestFile(secondsToWrite);

        simple(tmpFile);
        multiple(tmpFile);
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

    private static void simple(final Path file) throws IOException, ExecutionException, InterruptedException {
        System.out.println("# Simple read");

        try (RandomAccessCsvReader reader = RandomAccessCsvReader.builder().build(file)) {
            System.out.println("Wait until file has been completely indexed");

            final int size = reader.size().get();
            System.out.format("Indexed %,d rows%n", size);

            final CsvRow lastRow = reader.readRow(size - 1).get();
            System.out.println("Last row: " + lastRow);

            final CsvRow firstRow = reader.readRow(0).get();
            System.out.println("First row: " + firstRow);
        }

        System.out.println();
    }

    private static void multiple(final Path file) throws IOException, ExecutionException, InterruptedException {
        System.out.println("# Multiple read");

        final int firstRow = 5_000;
        final int noOfRows = 10;

        try (RandomAccessCsvReader reader = RandomAccessCsvReader.builder().build(file)) {
            final CompletableFuture<Stream<CsvRow>> rows = reader.readRows(firstRow, noOfRows);
            rows.get().forEach(System.out::println);
        }

        System.out.println();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static void statusMonitor(final Path file) throws IOException, InterruptedException, ExecutionException {
        System.out.printf("# Read file with a total of %,d bytes%n", Files.size(file));

        final StatusListener statusListener = new CountingStatusListener();

        final RandomAccessCsvReader csv = RandomAccessCsvReader.builder()
            .statusListener(statusListener)
            .build(file);

        try (csv) {
            // Indexing takes place in background – we can easily monitor the current status without blocking
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
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

        final CsvRow csvRow = RandomAccessCsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .build(file)
            .readRow(2)
            .get();

        System.out.println("Parsed via advanced config: " + csvRow);
    }

}
