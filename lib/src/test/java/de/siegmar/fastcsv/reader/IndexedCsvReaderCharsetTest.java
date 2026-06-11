package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Regression tests for the byte-oriented [CsvScanner] / [IndexedCsvReader] combination,
/// which assumes that the structural ASCII characters are encoded as single bytes.
/// Multibyte-everything charsets such as UTF-16 / UTF-32 corrupt both indexing and decoding
/// and therefore must be rejected.
class IndexedCsvReaderCharsetTest {

    @TempDir
    private Path tmpDir;

    @ParameterizedTest
    @ValueSource(strings = {"UTF-16LE", "UTF-16BE", "UTF-32LE", "UTF-32BE"})
    void rejectsUserSuppliedNonAsciiCompatibleCharset(final String charsetName) throws IOException {
        final Charset charset = Charset.forName(charsetName);
        final Path file = writeFile("rec1\nrec2\nrec3\nrec4\nrec5", charset);

        assertThatThrownBy(() -> IndexedCsvReader.builder().ofCsvRecord(file, charset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(charset.name());
    }

    @Test
    void rejectsBomDetectedUtf16Le() throws IOException {
        // FF FE BOM -> BomUtil detects UTF-16LE; build() with the default UTF-8 must still reject it.
        final Path file = writeBomFile(new byte[]{(byte) 0xFF, (byte) 0xFE},
            "rec1\nrec2\nrec3\nrec4\nrec5", StandardCharsets.UTF_16LE);

        assertThatThrownBy(() -> IndexedCsvReader.builder().ofCsvRecord(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("UTF-16LE");
    }

    @Test
    void rejectsBomDetectedUtf16Be() throws IOException {
        // FE FF BOM -> BomUtil detects UTF-16BE; build() with the default UTF-8 must still reject it.
        final Path file = writeBomFile(new byte[]{(byte) 0xFE, (byte) 0xFF},
            "a,\"b\nc\"", StandardCharsets.UTF_16BE);

        assertThatThrownBy(() -> IndexedCsvReader.builder().ofCsvRecord(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("UTF-16BE");
    }

    @Test
    void acceptsAsciiCompatibleCharset() throws IOException {
        final Path file = writeFile("abc\nüöä\nabc", StandardCharsets.UTF_8);

        assertThatCode(() -> {
            try (var csv = IndexedCsvReader.builder().ofCsvRecord(file, StandardCharsets.UTF_8)) {
                csv.readPage(0);
            }
        }).doesNotThrowAnyException();
    }

    private Path writeFile(final String data, final Charset charset) throws IOException {
        return Files.writeString(tmpDir.resolve("test.csv"), data, charset);
    }

    private Path writeBomFile(final byte[] bom, final String data, final Charset charset) throws IOException {
        final var bytes = new ByteArrayOutputStream();
        bytes.writeBytes(bom);
        bytes.writeBytes(data.getBytes(charset));
        return Files.write(tmpDir.resolve("test-bom.csv"), bytes.toByteArray());
    }

}
