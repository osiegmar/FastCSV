package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.CsvReader;
import testutil.CsvRecordAssert;

class BomHeaderTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder()
        .detectBomHeader(true);

    @ParameterizedTest
    @MethodSource
    void bom(final Path testFile) throws IOException {
        assertThat(crb.build(testFile).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .isEqualTo(List.of("foo", "üÜß"));
    }

    static List<Path> bom() {
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
