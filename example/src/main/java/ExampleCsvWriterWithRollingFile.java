import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.siegmar.fastcsv.writer.CsvWriter;

/// Example for writing CSV data to rolling files.
///
/// This can be handy if you want to split large data sets into multiple files, where each file contains only
/// a certain number of records or a certain amount of data.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() throws IOException {
    final int maxRecords = 500;
    final int maxFileSize = 1024;
    final String tmpPrefix = "fastcsv";
    final String filePattern = "example-%d.csv";

    // Create a file supplier that creates files in a temporary directory
    final var fileSupplier = new FileSupplier(Files.createTempDirectory(tmpPrefix), filePattern);

    // Policy to roll before hitting the maximum number of records or file size
    final RollingPolicy rollingPolicy = (recordsWritten, bytesWritten, bufferSize) ->
        recordsWritten >= maxRecords || bytesWritten + bufferSize > maxFileSize;

    // Consumer for processed files (e.g., for further processing)
    final Consumer<ProcessedFile> fileConsumer = (ProcessedFile file) ->
        IO.println("Rolled file: " + file);

    // Enable auto-flushing to ensure that records are written immediately to the output stream,
    // to ensure everything is written to the output stream before rolling the file.
    final var csvWriterBuilder = CsvWriter.builder()
        .autoFlush(true);

    try (
        var out = new RollingOutputStream(fileSupplier, rollingPolicy, fileConsumer);
        var csv = csvWriterBuilder.build(out)
    ) {

        for (int i = 0; i < 1000; i++) {
            csv.writeRecord("a", "b", "c");

            // Tell the rolling output stream that a record has been completely written
            out.done();
        }
    }
}

static class RollingOutputStream extends OutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Supplier<Path> fileSupplier;
    private final RollingPolicy rollingPolicy;
    private final Consumer<ProcessedFile> fileConsumer;
    private int recordCounter;
    private long currentSize;
    private Path currentFile;
    private OutputStream out;

    /// Creates a new rolling output stream.
    ///
    /// @param fileSupplier  a supplier for the next file to write to
    /// @param rollingPolicy the rolling policy
    /// @param fileConsumer  a consumer for rolled files (e.g., for further processing)
    public RollingOutputStream(final Supplier<Path> fileSupplier, final RollingPolicy rollingPolicy,
                               final Consumer<ProcessedFile> fileConsumer) {
        this.fileSupplier = Objects.requireNonNull(fileSupplier);
        this.rollingPolicy = Objects.requireNonNull(rollingPolicy);
        this.fileConsumer = Objects.requireNonNull(fileConsumer);
    }

    @Override
    public void write(final int b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        buffer.write(b, off, len);
    }

    // Signal that a record has been written
    public void done() throws IOException {
        if (recordCounter > 0 && rollingPolicy.shouldRoll(recordCounter, currentSize, buffer.size())) {
            close();
        }

        if (out == null) {
            initFile();
        }

        buffer.writeTo(out);

        recordCounter++;
        currentSize += buffer.size();

        buffer.reset();
    }

    private void initFile() throws IOException {
        currentFile = fileSupplier.get();
        out = new BufferedOutputStream(Files.newOutputStream(currentFile));
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }
        if (recordCounter > 0) {
            fileConsumer.accept(new ProcessedFile(currentFile, recordCounter, currentSize));
            recordCounter = 0;
            currentSize = 0;
        }
    }

}

@FunctionalInterface
interface RollingPolicy {

    /// Determines whether a roll should be performed.
    ///
    /// @param recordsWritten the number of records written so far
    /// @param bytesWritten   the number of bytes written so far
    /// @param bufferSize     the size of the current buffer (yet unwritten data)
    /// @return `true` if a roll should be performed
    boolean shouldRoll(int recordsWritten, long bytesWritten, int bufferSize);

}

static class FileSupplier implements Supplier<Path> {

    private final Path baseDir;
    private final String filePattern;
    private int fileCounter;

    public FileSupplier(final Path baseDir, final String filePattern) {
        this.baseDir = Objects.requireNonNull(baseDir);
        this.filePattern = Objects.requireNonNull(filePattern);
    }

    @Override
    public Path get() {
        return baseDir.resolve(filePattern.formatted(fileCounter++));
    }

}

record ProcessedFile(Path path, int records, long size) {
}
