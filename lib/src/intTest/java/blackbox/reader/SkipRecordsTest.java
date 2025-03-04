package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.RecordWrapper;
import testutil.CsvRecordAssert;

class SkipRecordsTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

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
        crb.quoteCharacter('\'');
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
        assertThat(crb.build(cbh, input).stream()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {",\nfoo\n", ",,\nfoo\n", "''\nfoo\n", "' '\nfoo\n"})
    void notEmptyCustomCallback(final String input) {
        crb.quoteCharacter('\'');
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
            protected RecordWrapper<String[]> buildRecord() {
                return wrapRecord(fields.toArray(new String[0]));
            }
        };
        assertThat(crb.build(cbh, input).stream()).hasSize(2);
    }

}
