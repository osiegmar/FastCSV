package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import testutil.CsvRecordAssert;

class CsvReaderBomHeaderTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder()
        .detectBomHeader(true);

    @ParameterizedTest
    @MethodSource("bomFile")
    void bomInputStream(final Path file) throws IOException {
        try (var in = Files.newInputStream(file, StandardOpenOption.READ)) {
            assertData(crb.ofCsvRecord(in));
        }
    }

    private static void assertData(final CsvReader<CsvRecord> csvReader) {
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> CsvRecordAssert.assertThat(c).fields().containsExactly("foo", "üÜß"),
                c -> CsvRecordAssert.assertThat(c).fields().containsExactly("123", "456")
            );
    }

    @ParameterizedTest
    @MethodSource
    void bomFile(final Path testFile) throws IOException {
        assertData(crb.ofCsvRecord(testFile));
    }

    static List<Path> bomFile() {
        final Path base = Path.of("src/intTest/resources");
        return List.of(
            base.resolve("utf8_nobom.csv"),
            base.resolve("utf8_bom.csv"),
            base.resolve("utf16be_bom.csv"),
            base.resolve("utf16le_bom.csv"),
            base.resolve("utf32be_bom.csv"),
            base.resolve("utf32le_bom.csv")
        );
    }

}
