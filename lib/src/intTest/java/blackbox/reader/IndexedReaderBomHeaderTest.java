package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.IndexedCsvReader;
import testutil.CsvRecordAssert;

class IndexedReaderBomHeaderTest {

    private final IndexedCsvReader.IndexedCsvReaderBuilder crb = IndexedCsvReader.builder();

    @ParameterizedTest
    @MethodSource("blackbox.reader.CsvReaderBomHeaderTest#bomFile")
    void bom(final Path testFile) throws IOException {
        try (var index = crb.ofCsvRecord(testFile)) {
            assertThat(index.getIndex()).satisfies(i -> assertThat(i.pages().size()).isOne());
            assertThat(index.readPage(0))
                .satisfiesExactly(
                    c -> CsvRecordAssert.assertThat(c).fields().containsExactly("foo", "üÜß"),
                    c -> CsvRecordAssert.assertThat(c).fields().containsExactly("123", "456")
                );
        }
    }

}
