package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;

class CsvCallbackHandlerTest {

    @Test
    void defaultMaxFieldCapacity() {
        final int max = 16_384;
        final String csv = ",".repeat(max - 1) + "\n" + ",".repeat(max);
        assertThatCode(() -> CsvReader.builder().ofCsvRecord(csv).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Record starting at line 2 has surpassed the maximum limit of 16384 fields")
            .hasNoCause();
    }

    @Test
    void changeMaxFieldCapacity() {
        final int max = 10;
        final String csv = ",".repeat(max - 1) + "\n" + ",".repeat(max);
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c
            .maxFields(max)
        );
        assertThatCode(() -> CsvReader.builder().build(cbh, csv).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Record starting at line 2 has surpassed the maximum limit of 10 fields")
            .hasNoCause();
    }

    @Test
    void invalidMaxFields() {
        assertThatCode(() -> CsvRecordHandler.of(c -> c.maxFields(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxFields must be > 0");
    }

    @Test
    void invalidMaxFieldSize() {
        assertThatCode(() -> CsvRecordHandler.of(c -> c.maxFieldSize(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxFieldSize must be > 0");
    }

    @Test
    void invalidMaxRecordSize() {
        assertThatCode(() -> CsvRecordHandler.of(c -> c.maxRecordSize(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxRecordSize must be > 0");
    }

    @Test
    void invalidMaxRecordSize2() {
        assertThatCode(() -> CsvRecordHandler.of(c -> c.maxRecordSize(1).maxFieldSize(2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxRecordSize must be >= maxFieldSize");
    }

    @Test
    void changeMaxRecordSize() {
        final String csv = "01,23,45,67,89";
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c
            .maxFieldSize(2)
            .maxRecordSize(5)
        );
        assertThatCode(() -> CsvReader.builder().build(cbh, csv).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Field at index 2 in record starting at line 1 exceeds the max record size of 5 characters")
            .hasNoCause();
    }

    @Test
    void changeMaxFieldSize() {
        final String csv = "0123456789";
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c
            .maxFieldSize(5)
        );
        assertThatCode(() -> CsvReader.builder().build(cbh, csv).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Field at index 0 in record starting at line 1 exceeds the max field size of 5 characters")
            .hasNoCause();
    }

    @Test
    void changeMaxFieldSizeForComments() {
        final String csv = "#0123456789";
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c
            .maxFieldSize(5)
        );
        final var builder = CsvReader.builder().commentStrategy(CommentStrategy.READ);
        assertThatCode(() -> builder.build(cbh, csv).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Field at index 0 in record starting at line 1 exceeds the max field size of 5 characters")
            .hasNoCause();
    }

}
