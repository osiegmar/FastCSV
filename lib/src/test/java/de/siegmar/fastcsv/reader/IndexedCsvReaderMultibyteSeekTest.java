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

    @TempDir
    private Path tmpDir;

    @Test
    void multibyteCharStraddlingReadBoundaryAfterSeek() throws IOException {
        // The 'é' (2 bytes in UTF-8) is positioned so that it straddles the 8191/8192 byte boundary
        // of the decoder's internal read.
        final String firstLine = "a".repeat(100);
        final String secondField = "b".repeat(8090) + "é" + "c".repeat(50);
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
