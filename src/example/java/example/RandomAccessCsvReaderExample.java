package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.RandomAccessCsvReader;
import de.siegmar.fastcsv.reader.StatusMonitor;
import de.siegmar.fastcsv.writer.CsvWriter;

public class RandomAccessCsvReaderExample {

    public static void main(final String[] args) throws Exception {
        final int secondsToWrite = 3;
        final Path tmpFile = prepareTestFile(secondsToWrite);

        simple(tmpFile);
        multiple(tmpFile);
        statusMonitor(tmpFile);
    }

    private void foo() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Path file = Paths.get("");
        try (RandomAccessCsvReader csvReader = RandomAccessCsvReader.builder().build(file)) {
            CsvRow row = csvReader.readRow(3000)
                .get(1, TimeUnit.SECONDS);
            System.out.println(row);
        }
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
        final long fileSize = Files.size(file);
        System.out.printf("# Read file with a total of %,d bytes%n", fileSize);

        try (RandomAccessCsvReader reader = RandomAccessCsvReader.builder().build(file)) {
            // Indexing takes place in background – we can easily monitor the current status without blocking
            final StatusMonitor statusMonitor = reader.getStatusMonitor();

            do {
                final long readBytes = statusMonitor.getReadBytes();
                System.out.format("Read positions: %,d; read bytes: %,d (%.1f %%)%n",
                    statusMonitor.getPositionCount(), readBytes, 100.0 / fileSize * readBytes);
            } while (!reader.awaitIndex(250, TimeUnit.MILLISECONDS));

            System.out.printf("Finished reading file with a total of %,d records%n", reader.size().get());
        }

        System.out.println();
    }

}
