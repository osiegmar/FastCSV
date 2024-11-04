package blackbox.reader;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.CsvRecordAssert;
import testutil.NamedCsvRecordAssert;

class SkipLinesTest {

    // Skip lines based on the count

    @Test
    void noInput() {
        assertThatCode(() -> CsvReader.builder().ofCsvRecord("A\r\nB\r\nC").skipLines(0))
            .doesNotThrowAnyException();
    }

    @Test
    void negativeInput() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("A\r\nB\r\nC");
        assertThatThrownBy(() -> csv.skipLines(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("lineCount must be non-negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A\r\nB\r\nC", "A\nB\nC", "A\rB\rC"})
    void skipLinesWithCount(final String input) {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(input);

        csv.skipLines(2);

        assertThat(csv.stream())
            .singleElement()
            .satisfies(rec -> CsvRecordAssert.assertThat(rec)
                .isStartingLineNumber(3).fields().containsExactly("C"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A\r\nB\r\nC", "A\nB\nC", "A\rB\rC"})
    void tooMany(final String input) {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(input);

        assertThatThrownBy(() -> csv.skipLines(4))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Not enough lines to skip. Skipped only 3 line(s).");

        assertThat(csv.stream())
            .isEmpty();
    }

    @Test
    void countIoException() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(new UnreadableReader());
        assertThatThrownBy(() -> csv.skipLines(1))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot read");
    }

    // Skip lines based on a predicate

    @Test
    void noPredicate() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("");
        assertThatThrownBy(() -> csv.skipLines(null, 0))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("predicate must not be null");
    }

    @Test
    void negativeMaxLines() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(line -> true, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxLines must be non-negative");
    }

    @Test
    void zeroMaxLines() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("A\nB");
        csv.skipLines(line -> false, 0);
        assertThat(csv.stream())
            .satisfiesExactly(
                rec1 -> CsvRecordAssert.assertThat(rec1)
                    .isStartingLineNumber(1).fields().containsExactly("A"),
                rec2 -> CsvRecordAssert.assertThat(rec2)
                    .isStartingLineNumber(2).fields().containsExactly("B")
            );
    }

    @Test
    void reachedMaxLines() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(line -> false, 1))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("No matching line found within the maximum limit of 1 lines.");
    }

    @Test
    void noMatch() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(line -> false, 10))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("No matching line found. Skipped 2 line(s) before reaching end of data.");
    }

    @ValueSource(strings = {
        "some arbitrary text\r\nbefore the actual data\r\n\r\nheader1,header2\r\nvalue1,value2\r\n",
        "some arbitrary text\rbefore the actual data\r\rheader1,header2\rvalue1,value2\r",
        "some arbitrary text\rbefore the actual data\n\nheader1,header2\nvalue1,value2\n"
    })
    @ParameterizedTest
    void skipLinesWithPredicate(final String data) {
        final CsvReader<NamedCsvRecord> csv = CsvReader.builder()
            .ofNamedCsvRecord(data);

        csv.skipLines(line -> line.contains("header1"), 10);

        assertThat(csv.stream())
            .singleElement()
            .satisfies(rec -> NamedCsvRecordAssert.assertThat(rec)
                .isStartingLineNumber(5).fields()
                .containsExactly(entry("header1", "value1"), entry("header2", "value2")));
    }

    @Test
    void predicateIoException() {
        final CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(new UnreadableReader());
        assertThatThrownBy(() -> csv.skipLines(line -> true, 1))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot read");
    }

}
