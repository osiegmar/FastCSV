package example;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for implementing a custom callback handler.
///
/// You should only go this route if you need to squeeze out every bit of performance and I/O or post-processing is not
/// a bottleneck.
/// The standard implementation ([de.siegmar.fastcsv.reader.CsvRecordHandler]) is already very fast and should be
/// sufficient for most use cases.
///
/// A comparison with 1 bn records (86 GiB) has shown the following results:
///
/// | Description                                              | Duration (throughput)                                |
/// |----------------------------------------------------------|------------------------------------------------------|
/// | Standard stream-based Mapper (with standard Java Parser) | 11m 48s (1.41 M records/s) – baseline                |
/// | Standard stream-based Mapper (with FastNumberParser)     | 4m 25s (3.77 M records/s) – 63% faster than baseline |
/// | Custom Mapper (with FastNumberParser)                    | 3m 2s (5.49 M records/s) – 74% faster than baseline  |
///
/// As you can see, the biggest impact on performance has the number parser.
public class ExampleCsvReaderWithCustomCallbackHandler {

    /// 1 million records creates a temporary file of about 72 MiB.
    ///
    /// Of course, this is definitely not large enough to justify a custom callback handler.
    /// I just don't want to fill up your disk with a terabyte of data by default. 😇
    private static final int RECORDS_TO_PRODUCE = 1_000_000;

    private static final Random RND = new Random();

    public static void main(final String[] args) throws IOException {
        // prepare a large fake dataset with temperature measurements
        final Path testFile = produceLargeFakeDataset();

        System.out.println("Mapping data with stream handler:");
        read(() -> streamMapper(testFile, mapWithStandardDoubleParser()));

        System.out.println("Mapping data with stream handler (and FastNumberParser):");
        read(() -> streamMapper(testFile, mapWithFastNumberParser()));

        System.out.println("Mapping data with custom callback handler (and FastNumberParser):");
        read(() -> customMapper(testFile));
    }

    private static void read(final Supplier<Stream<Measurement>> streamSupplier) {
        final LocalDateTime start = LocalDateTime.now();
        try (Stream<Measurement> stream = streamSupplier.get()) {
            System.out.printf("Duration to map %,d records: %s%n%n",
                stream.count(), Duration.between(start, LocalDateTime.now()));
        }
    }

    private static Stream<Measurement> customMapper(final Path testFile) {
        try {
            return CsvReader.builder().build(new MeasurementCallbackHandler(), testFile).stream();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Measurement> streamMapper(final Path testFile,
                                                    final Function<NamedCsvRecord, Measurement> mapper) {
        try {
            return CsvReader.builder().ofNamedCsvRecord(testFile).stream().map(mapper);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Function<NamedCsvRecord, Measurement> mapWithStandardDoubleParser() {
        return record -> new Measurement(
            Long.parseLong(record.getField("ID").substring(3)),
            Long.parseLong(record.getField("Timestamp")),
            Double.parseDouble(record.getField("Latitude")),
            Double.parseDouble(record.getField("Longitude")),
            Double.parseDouble(record.getField("Temperature")));
    }

    private static Function<NamedCsvRecord, Measurement> mapWithFastNumberParser() {
        return record -> new Measurement(
            FastNumberParser.parseLong(record.getField("ID").substring(3)),
            FastNumberParser.parseLong(record.getField("Timestamp")),
            FastNumberParser.parseDouble(record.getField("Latitude")),
            FastNumberParser.parseDouble(record.getField("Longitude")),
            FastNumberParser.parseDouble(record.getField("Temperature")));
    }

    /// Produces a fake dataset with the given number of records.
    ///
    /// The file contains a header and 5 columns:
    ///
    /// - ID
    /// - Timestamp
    /// - Longitude
    /// - Latitude
    /// - TemperatureUnit
    /// - Temperature
    ///
    /// The file will be deleted on JVM exit.
    ///
    /// @return path to the created file
    /// @throws IOException if an I/O error occurs
    private static Path produceLargeFakeDataset() throws IOException {
        final Path tmpFile = Files.createTempFile("fastcsv", ".csv");
        tmpFile.toFile().deleteOnExit();

        final long currentTime = System.currentTimeMillis();
        final long yearInMillis = Duration.ofDays(365).toMillis();

        System.out.printf("Creating file %s with %,d records...%n", tmpFile, RECORDS_TO_PRODUCE);

        try (CsvWriter csv = CsvWriter.builder().build(tmpFile)) {
            csv.writeRecord("ID", "Timestamp", "Longitude", "Latitude", "TemperatureUnit", "Temperature");

            for (int i = 0; i < RECORDS_TO_PRODUCE; i++) {
                final String measuringStationId = "ID-" + i;
                final long timestamp = currentTime - RND.nextLong(yearInMillis);
                final double latitude = RND.nextDouble() * 180 - 90;
                final double longitude = RND.nextDouble() * 360 - 180;
                final double temperature = RND.nextDouble() * 150 - 90;

                csv.writeRecord(measuringStationId,
                    Long.toString(timestamp),
                    Double.toString(latitude),
                    Double.toString(longitude),
                    "Celsius",
                    Double.toString(temperature));
            }
        }

        System.out.printf("File %s with %,d records and %,d bytes created%n%n",
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
                case 1 -> timestamp = FastNumberParser.parseLong(buf, offset, len);
                case 2 -> latitude = FastNumberParser.parseDouble(buf, offset, len);
                case 3 -> longitude = FastNumberParser.parseDouble(buf, offset, len);
                case 4 -> {
                    // Skip temperature unit
                }
                case 5 -> temperature = FastNumberParser.parseDouble(buf, offset, len);
                default -> throw new IllegalStateException("Unexpected column: %d, starting line: %d"
                    .formatted(fieldIdx, getStartingLineNumber()));
            }
        }

        private long materializeId(final char[] buf, final int offset, final int len) {
            final int prefixLength = "ID-".length();
            if (len <= prefixLength) {
                throw new IllegalStateException("ID too short: %d, starting line: %d"
                    .formatted(len, getStartingLineNumber()));
            }
            return FastNumberParser.parseLong(buf, offset + prefixLength, len - prefixLength);
        }

        @Override
        protected Measurement buildRecord() {
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
    }

    // Use JavaDoubleParser instead of Double.parseDouble() for way better performance
    // see https://github.com/wrandelshofer/FastDoubleParser
    private static final class FastNumberParser {

        private static long parseLong(final String str) {
            long result = 0;
            for (int i = 0; i < str.length(); i++) {
                result = result * 10 + str.charAt(i) - '0';
            }

            return result;
        }

        private static long parseLong(final char[] buf, final int offset, final int len) {
            long result = 0;
            for (int i = 0; i < len; i++) {
                result = result * 10 + buf[offset + i] - '0';
            }

            return result;
        }

        private static double parseDouble(final String str) {
            return JavaDoubleParser.parseDouble(str);
        }

        private static double parseDouble(final char[] buf, final int offset, final int len) {
            return JavaDoubleParser.parseDouble(buf, offset, len);
        }

    }

}
