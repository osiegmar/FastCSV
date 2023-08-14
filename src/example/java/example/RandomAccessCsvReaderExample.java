package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.RandomAccessCsvReader;
import de.siegmar.fastcsv.reader.StatusMonitor;
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

        System.out.format("Temporary test file with %,d records and %,d bytes successfully prepared%n%n",
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
            System.out.format("Indexed %,d records%n", size);

            final CsvRow lastRow = reader.readRow(size - 1).get();
            System.out.println("Last row: " + lastRow);

            final CsvRow firstRow = reader.readRow(0).get();
            System.out.println("First row: " + firstRow);
        }

        System.out.println();
    }

    private static void multiple(final Path file) throws IOException {
        System.out.println("# Multiple read");

        final int firstRecord = 5_000;
        final int noOfRecords = 10;

        try (RandomAccessCsvReader reader = RandomAccessCsvReader.builder().build(file)) {
            reader.readRows(firstRecord, noOfRecords, System.out::println).join();
        }

        System.out.println();
    }

    private static void statusMonitor(final Path file) throws IOException, InterruptedException, ExecutionException {
        System.out.printf("# Read file with a total of %,d bytes%n", Files.size(file));

        try (RandomAccessCsvReader csv = RandomAccessCsvReader.builder().build(file)) {
            // Indexing takes place in background – we can easily monitor the current status without blocking
            final StatusMonitor statusMonitor = csv.getStatusMonitor();

            do {
                // Print current status
                System.out.println(statusMonitor);
            } while (!csv.awaitIndex(250, TimeUnit.MILLISECONDS));

            System.out.printf("Finished reading file with a total of %,d records%n%n", csv.size().get());
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
