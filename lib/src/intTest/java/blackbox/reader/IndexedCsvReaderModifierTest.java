package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import testutil.CsvRecordAssert;

class IndexedCsvReaderModifierTest {

    private final IndexedCsvReader.IndexedCsvReaderBuilder crb = IndexedCsvReader.builder();

    @TempDir
    private Path tmpDir;

    @Test
    void trim() throws IOException {
        final var file = Files.writeString(tmpDir.resolve("indexed_field_modifier_test.csv"),
            "foo  ,  bar",
            StandardCharsets.UTF_8);

        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));

        final var build = crb.build(cbh, file);
        try (build) {
            assertThat(build.readPage(0))
                .singleElement(CsvRecordAssert.CSV_RECORD)
                .fields().containsExactly("foo", "bar");
        }
    }

}
