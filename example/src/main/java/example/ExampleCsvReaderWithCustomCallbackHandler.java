package example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Example for implementing a custom callback handler.
 * <p>
 * You should only go this route if you need to squeeze out every bit of performance and I/O or post-processing is not
 * the bottleneck. The standard implementation ({@link de.siegmar.fastcsv.reader.CsvRecordHandler}) is already
 * very fast and should be sufficient for most use cases.
 */
public class ExampleCsvReaderWithCustomCallbackHandler {

    /**
     * 1 million records creates a temporary file of about 72 MiB.
     * <p>
     * Of course, this is definitely not large enough to justify a custom callback handler.
     * I just don't want to fill up your disk with a terabyte of data by default. 😇
     */
    private static final int RECORDS_TO_PRODUCE = 1_000_000;

    private static final Random RND = new Random();

    public static void main(final String[] args) throws IOException {
        System.out.println("Mapping data with custom callback handler:");

        // prepare a large fake dataset with temperature measurements
        final Path testFile = produceLargeFakeDataset();

        final LocalDateTime start = LocalDateTime.now();

        try (CsvReader<Measurement> csv = CsvReader.builder().build(new MeasurementCallbackHandler(), testFile)) {
            System.out.println("Youngest measurements:");

            // Show the 3 youngest measurements
            csv.stream()
                .sorted((m1, m2) -> Long.compare(m2.timestamp, m1.timestamp))
                .limit(3)
                .forEach(System.out::println);
        }

        System.out.printf("Duration to map %,d records: %s%n",
            RECORDS_TO_PRODUCE, Duration.between(start, LocalDateTime.now()));
    }

    /**
     * Produces a fake dataset with the given number of records.
     * <p>
     * The file contains a header and 5 columns:
     * <ul>
     *     <li>ID</li>
     *     <li>Timestamp</li>
     *     <li>Longitude</li>
     *     <li>Latitude</li>
     *     <li>TemperatureUnit</li>
     *     <li>Temperature</li>
     * </ul>
     * <p>
     * The file will be deleted on JVM exit.
     *
     * @return path to the created file
     * @throws IOException if an I/O error occurs
     */
    private static Path produceLargeFakeDataset() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();

        final long currentTime = System.currentTimeMillis();
        final long yearInMillis = Duration.ofDays(365).toMillis();

        try (CsvWriter csv = CsvWriter.builder().build(tmpFile)) {
            csv.writeRecord("ID", "Timestamp", "Longitude", "Latitude", "TemperatureUnit", "Temperature");

            for (int i = 0; i < RECORDS_TO_PRODUCE; i++) {
                final String measuringStationId = "ID-" + i;
                final long timestamp = currentTime - RND.nextLong(yearInMillis);
                final double latitude = RND.nextDouble() * 180 - 90;
                final double longitude = RND.nextDouble() * 360 - 180;
                final double temperature = RND.nextDouble() * 150 - 90;

                csv.writeRecord(measuringStationId,
                    String.valueOf(timestamp),
                    String.valueOf(latitude),
                    String.valueOf(longitude),
                    "Celcius",
                    String.valueOf(temperature));
            }
        }

        System.out.printf("File %s with %,d records and %,d bytes created%n",
            tmpFile, ExampleCsvReaderWithCustomCallbackHandler.RECORDS_TO_PRODUCE, Files.size(tmpFile));

        return tmpFile;
    }

    private static final class MeasurementCallbackHandler extends AbstractBaseCsvCallbackHandler<Measurement> {

        private long recordCount;

        private long id;
        private long timestamp;
        private double latitude;
        private double longitude;
        private double temperature;

        @SuppressWarnings("checkstyle:InnerAssignment")
        @Override
        public void handleField(final int fieldIdx, final char[] buf, final int offset, final int len,
                                final boolean quoted) {

            if (recordCount == 0) {
                // Skip header
                return;
            }

            // As we're implementing a custom callback handler, we have to check length constraints ourselves
            if (len > 100) {
                throw new IllegalStateException("Field too long: %d, starting line: %d"
                    .formatted(len, getStartingLineNumber()));
            }

            // We expect fields are: ID, Timestamp, Latitude, Longitude, TemperatureUnit, Temperature

            switch (fieldIdx) {
                case 0 -> id = materializeId(buf, offset, len);
                case 1 -> timestamp = parseLong(buf, offset, len);
                case 2 -> latitude = parseDouble(buf, offset, len);
                case 3 -> longitude = parseDouble(buf, offset, len);
                case 4 -> {
                    // Skip temperature unit
                }
                case 5 -> temperature = parseDouble(buf, offset, len);
                default ->
                    throw new IllegalStateException("Unexpected column: %d, starting line: %d"
                        .formatted(fieldIdx, getStartingLineNumber()));
            }
        }

        private long materializeId(final char[] buf, final int offset, final int len) {
            final int prefixLength = "ID-".length();
            if (len <= prefixLength) {
                throw new IllegalStateException("ID too short: %d, starting line: %d"
                    .formatted(len, getStartingLineNumber()));
            }
            return parseLong(buf, offset + prefixLength, len - prefixLength);
        }

        private static long parseLong(final char[] buf, final int offset, final int len) {
            return Long.parseLong(new String(buf, offset, len));
        }

        private static double parseDouble(final char[] buf, final int offset, final int len) {
            //return Double.parseDouble(new String(buf, offset, len));

            // Use JavaDoubleParser instead of Double.parseDouble() for way better performance
            // see https://github.com/wrandelshofer/FastDoubleParser
            return JavaDoubleParser.parseDouble(buf, offset, len);
        }

        @Override
        public Measurement build() {
            if (recordCount++ == 0) {
                // Skip header
                return null;
            }

            if (getFieldCount() != 6) {
                throw new IllegalStateException("Expected 6 fields, but got %d, starting line: %d"
                    .formatted(getFieldCount(), getStartingLineNumber()));
            }

            return new Measurement(id, timestamp, latitude, longitude, temperature);
        }

    }

    private record Measurement(long id, long timestamp, double latitude, double longitude, double temperature) {

        private static final String GOOGLE_MAPS_URL_TEMPLATE = "https://www.google.com/maps/place/%f,%f/@%f,%f,4z";

        double fahrenheit() {
            return temperature * 1.8 + 32;
        }

        // Wondering where this place would be? 😉
        String mapLocation() {
            return String.format(Locale.US, GOOGLE_MAPS_URL_TEMPLATE, latitude, longitude, latitude, longitude);
        }

        @Override
        public String toString() {
            return "Measured %.1f°C (%.1f°F) on station %d at %s - see: %s".formatted(
                temperature, fahrenheit(), id, Instant.ofEpochMilli(timestamp), mapLocation());
        }
    }

}
