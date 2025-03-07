package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NamedCsvRecordHandlerTest {

    private static final String TEST_DATA_W_HEADER = " col1 , col2 \n foo , bar ";
    private static final String TEST_DATA_NO_HEADER = " foo , bar ";

    @Test
    void fieldModifierAndHeaderConstructor() {
        final NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder()
            .fieldModifier(FieldModifiers.TRIM)
            .header("col1", "col2")
            .build();
        assertThat(CsvReader.builder().build(handler, TEST_DATA_NO_HEADER).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields().containsExactly(Map.entry("col1", "foo"), Map.entry("col2", "bar"));
    }

    @Test
    void defaultHandler() {
        final NamedCsvRecordHandler handler = NamedCsvRecordHandler.of();
        assertThat(CsvReader.builder().build(handler, TEST_DATA_W_HEADER).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields().containsExactly(Map.entry(" col1 ", " foo "), Map.entry(" col2 ", " bar "));
    }

    @Test
    void builder() {
        final NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder()
            .fieldModifier(FieldModifiers.TRIM)
            .build();
        assertThat(CsvReader.builder().build(handler, TEST_DATA_W_HEADER).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields().containsExactly(Map.entry("col1", "foo"), Map.entry("col2", "bar"));
    }

    @Test
    void consumer() {
        final NamedCsvRecordHandler handler = NamedCsvRecordHandler.of(c -> c
            .fieldModifier(FieldModifiers.TRIM)
        );
        assertThat(CsvReader.builder().build(handler, TEST_DATA_W_HEADER).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields().containsExactly(Map.entry("col1", "foo"), Map.entry("col2", "bar"));
    }

}
