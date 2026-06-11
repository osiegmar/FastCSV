package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// Regression test: a reused [java.io.InputStreamReader] retains undecoded leftover bytes when its
/// internal read ends mid-multibyte-sequence. After a [java.io.RandomAccessFile#seek] those stale
/// bytes used to be prepended to the next page's data, corrupting it.
class IndexedCsvReaderMultibyteSeekTest {

    /// Size of the JDK `StreamDecoder`'s internal byte buffer, i.e. how many bytes
    /// [java.io.InputStreamReader] pulls from the stream per read. This is a JDK implementation
    /// detail, not a public contract – the test only stays boundary-exact while it holds.
    private static final int DECODER_READ_SIZE = 8192;

    /// Length of the first line (an arbitrary, known-size prefix the seek skips over).
    private static final int FIRST_LINE_LENGTH = 100;

    /// Number of filler bytes before the multibyte char, chosen so the char's first byte lands on the
    /// last byte (offset `DECODER_READ_SIZE - 1`) of the decoder's first read and its second byte only
    /// arrives on the next read. Layout: `FIRST_LINE_LENGTH` + 1 (newline) + `FILLER_LENGTH` bytes
    /// precede the char, so its first byte sits at offset `FIRST_LINE_LENGTH + 1 + FILLER_LENGTH`.
    private static final int FILLER_LENGTH = DECODER_READ_SIZE - 1 - (FIRST_LINE_LENGTH + 1);

    /// Trailing field content after the multibyte char – arbitrary, just enough that the field
    /// continues past the straddling char and the assertion verifies the whole field survives.
    private static final int TRAILING_LENGTH = 50;

    @TempDir
    private Path tmpDir;

    @Test
    void multibyteCharStraddlingReadBoundaryAfterSeek() throws IOException {
        // 'é' is 2 bytes in UTF-8; it is positioned to straddle the decoder's read boundary
        // (first byte at offset DECODER_READ_SIZE - 1, second byte in the following read).
        final String firstLine = "a".repeat(FIRST_LINE_LENGTH);
        final String secondField = "b".repeat(FILLER_LENGTH) + "é" + "c".repeat(TRAILING_LENGTH);
        final String content = firstLine + "\n" + secondField + "\n";

        final Path file = tmpDir.resolve("multibyte.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        try (IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder()
            .pageSize(1)
            .ofCsvRecord(file)) {

            // Reading page 0 first leaves leftover bytes in the decoder; page 1 must not be corrupted.
            csv.readPage(0);

            final List<CsvRecord> page1 = csv.readPage(1);
            assertThat(page1)
                .singleElement()
                .extracting(rec -> rec.getField(0))
                .isEqualTo(secondField);
        }
    }

}
