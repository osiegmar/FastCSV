package example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing and reading compressed CSV data.
public class ExampleCsvCompression {

    public static void main(final String[] args) throws IOException {
        final Path file = Files.createTempFile("fastcsv", ".csv.gz");
        file.toFile().deleteOnExit();

        System.out.println(file.toAbsolutePath());

        writeCsvGzipped(file);
        readCsvGzipped(file);
    }

    private static void writeCsvGzipped(final Path file) throws IOException {
        try (var csv = CsvWriter.builder().build(gzipWriter(file))) {
            csv.writeRecord("header1", "header2");
            csv.writeRecord("value1", "value2");
        }
    }

    private static OutputStreamWriter gzipWriter(final Path file)
        throws IOException {
        final var bufOut = new BufferedOutputStream(Files.newOutputStream(file));
        final var gzipOut = new GZIPOutputStream(bufOut);
        return new OutputStreamWriter(gzipOut, UTF_8);
    }

    private static void readCsvGzipped(final Path file) throws IOException {
        System.out.println("Reading compressed CSV file:");

        try (var csv = CsvReader.builder().ofCsvRecord(gzipReader(file))) {
            csv.forEach(System.out::println);
        }
    }

    private static InputStream gzipReader(final Path file) throws IOException {
        return new GZIPInputStream(Files.newInputStream(file));
    }

}
