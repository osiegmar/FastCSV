package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.IndexedCsvReader;
import testutil.CsvRecordAssert;

class IndexedReaderBomHeaderTest {

    private static final Path RESOURCE_DIR = Path.of("src/intTest/resources");

    private final IndexedCsvReader.IndexedCsvReaderBuilder crb = IndexedCsvReader.builder();

    @ParameterizedTest
    @ValueSource(strings = {"utf8_nobom.csv", "utf8_bom.csv"})
    void bom(final String filename) throws IOException {
        try (var index = crb.ofCsvRecord(RESOURCE_DIR.resolve(filename))) {
            assertThat(index.getIndex()).satisfies(i -> assertThat(i.pages().size()).isOne());
            assertThat(index.readPage(0))
                .satisfiesExactly(
                    c -> CsvRecordAssert.assertThat(c).fields().containsExactly("foo", "üÜß"),
                    c -> CsvRecordAssert.assertThat(c).fields().containsExactly("123", "456")
                );
        }
    }

    // The byte-oriented index scanner only supports ASCII-compatible charsets. A BOM-detected
    // UTF-16/UTF-32 charset must therefore be rejected (see IndexedCsvReader#build).
    @ParameterizedTest
    @ValueSource(strings = {"utf16be_bom.csv", "utf16le_bom.csv", "utf32be_bom.csv", "utf32le_bom.csv"})
    void rejectsMultibyteBomCharset(final String filename) {
        assertThatThrownBy(() -> crb.ofCsvRecord(RESOURCE_DIR.resolve(filename)))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
