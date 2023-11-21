package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import testutil.NamedCsvRecordAssert;

class NamedCsvReaderBomHeaderTest {

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder()
        .detectBomHeader(true);

    @ParameterizedTest
    @MethodSource
    void bom(final Path testFile) throws IOException {
        assertThat(crb.build(testFile).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .satisfies(
                c -> assertThat(c.getField("foo")).isEqualTo("123"),
                c -> assertThat(c.getField("üÜß")).isEqualTo("456")
            );
    }

    static List<Path> bom() {
        return CsvReaderBomHeaderTest.bom();
    }

}
