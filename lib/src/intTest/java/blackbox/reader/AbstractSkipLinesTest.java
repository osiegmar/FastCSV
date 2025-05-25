package blackbox.reader;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.CsvRecordAssert;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings({
    "PMD.CloseResource",
    "PMD.AvoidDuplicateLiterals",
    "PMD.AbstractClassWithoutAbstractMethod"
})
abstract class AbstractSkipLinesTest {

    protected final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    // no skip

    @Test
    void singleRecordNoSkipEmpty() {
        crb.skipEmptyLines(false);
        assertThat(crb.ofCsvRecord("").iterator()).isExhausted();
    }

    @Test
    void multipleRecordsNoSkipEmpty() {
        crb.skipEmptyLines(false);

        assertThat(crb.ofCsvRecord("\n\na").iterator()).toIterable()
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isStartingLineNumber(1).fields().containsExactly(""),
                item2 -> CsvRecordAssert.assertThat(item2).isStartingLineNumber(2).fields().containsExactly(""),
                item3 -> CsvRecordAssert.assertThat(item3).isStartingLineNumber(3).fields().containsExactly("a"));
    }

    @ParameterizedTest
    @ValueSource(strings = {",\nfoo\n", ",,\nfoo\n", "''\nfoo\n", "' '\nfoo\n"})
    void notEmpty(final String input) {
        crb.allowMissingFields(true).quoteCharacter('\'');
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
        assertThat(crb.build(cbh, input).stream()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {",\nfoo\n", ",,\nfoo\n", "''\nfoo\n", "' '\nfoo\n"})
    void notEmptyCustomCallback(final String input) {
        crb.allowMissingFields(true).quoteCharacter('\'');
        final AbstractBaseCsvCallbackHandler<String[]> cbh = new AbstractBaseCsvCallbackHandler<>() {
            private final List<String> fields = new ArrayList<>();

            @Override
            protected void handleBegin(final long startingLineNumber) {
                fields.clear();
            }

            @Override
            protected void handleField(final int fieldIdx, final char[] buf,
                                       final int offset, final int len, final boolean quoted) {
                fields.add(new String(buf, offset, len).trim());
            }

            @Override
            protected String[] buildRecord() {
                return fields.toArray(new String[0]);
            }
        };
        assertThat(crb.build(cbh, input).stream()).hasSize(2);
    }

    // Skip lines based on the count

    @Test
    void noInput() {
        assertThatCode(() -> crb.ofCsvRecord("A\r\nB\r\nC").skipLines(0))
            .doesNotThrowAnyException();
    }

    @Test
    void negativeInput() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("A\r\nB\r\nC");
        assertThatThrownBy(() -> csv.skipLines(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("lineCount must be non-negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A\r\nB\r\nC", "A\nB\nC", "A\rB\rC"})
    void skipLinesWithCount(final String input) {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord(input);

        csv.skipLines(2);

        assertThat(csv.stream())
            .singleElement()
            .satisfies(rec -> CsvRecordAssert.assertThat(rec)
                .isStartingLineNumber(3).fields().containsExactly("C"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A\r\nB\r\nC", "A\nB\nC", "A\rB\rC"})
    void tooMany(final String input) {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord(input);

        assertThatThrownBy(() -> csv.skipLines(4))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Not enough lines to skip. Skipped only 3 line(s).");

        assertThat(csv.stream())
            .isEmpty();
    }

    @Test
    void countIoException() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord(new UnreadableReader());
        assertThatThrownBy(() -> csv.skipLines(1))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot read");
    }

    // Skip lines based on a predicate

    @Test
    void noPredicate() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("");
        assertThatThrownBy(() -> csv.skipLines(null, 0))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("predicate must not be null");
    }

    @Test
    void negativeMaxLines() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(_ -> true, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxLines must be non-negative");
    }

    @Test
    void zeroMaxLines() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("A\nB");
        csv.skipLines(_ -> false, 0);
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
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(_ -> false, 1))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("No matching line found within the maximum limit of 1 lines.");
    }

    @Test
    void noMatch() {
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord("A\nB");
        assertThatThrownBy(() -> csv.skipLines(_ -> false, 10))
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
        final CsvReader<NamedCsvRecord> csv = crb
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
        final CsvReader<CsvRecord> csv = crb.ofCsvRecord(new UnreadableReader());
        assertThatThrownBy(() -> csv.skipLines(_ -> true, 1))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot read");
    }

}
