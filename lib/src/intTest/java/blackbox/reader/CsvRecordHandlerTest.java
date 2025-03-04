package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import testutil.CsvRecordAssert;

class CsvRecordHandlerTest {

    private static final String TEST_DATA = " foo , bar ";

    @Test
    void defaultConstructor() {
        @SuppressWarnings("removal")
        final CsvRecordHandler handler = new CsvRecordHandler();
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly(" foo ", " bar ");
    }

    @Test
    void fieldModifierConstructor() {
        @SuppressWarnings("removal")
        final CsvRecordHandler handler = new CsvRecordHandler(FieldModifiers.TRIM);
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly("foo", "bar");
    }

    @Test
    void defaultHandler() {
        final CsvRecordHandler handler = CsvRecordHandler.of();
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly(" foo ", " bar ");
    }

    @Test
    void builder() {
        final CsvRecordHandler handler = CsvRecordHandler.builder()
            .fieldModifier(FieldModifiers.TRIM)
            .build();
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly("foo", "bar");
    }

    @Test
    void consumer() {
        final CsvRecordHandler handler = CsvRecordHandler.of(c -> c
            .fieldModifier(FieldModifiers.TRIM)
        );
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly("foo", "bar");
    }

}
