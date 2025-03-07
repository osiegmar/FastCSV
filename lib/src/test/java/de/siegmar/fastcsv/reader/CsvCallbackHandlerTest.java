package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import testutil.CsvRecordAssert;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CsvCallbackHandlerTest {

    @Test
    void csvRecord() {
        final CsvCallbackHandler<CsvRecord> rh = CsvRecordHandler.of();
        process(rh);

        CsvRecordAssert.assertThat(rh.buildRecord())
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

    @Test
    void namedCsvRecord() {
        final CsvCallbackHandler<NamedCsvRecord> rh = NamedCsvRecordHandler.of();

        rh.beginRecord(1);
        addField(rh, "head1");
        addField(rh, "head2");
        rh.buildRecord();

        rh.beginRecord(2);
        addField(rh, "foo");
        addField(rh, "bar");

        NamedCsvRecordAssert.assertThat(rh.buildRecord())
            .isStartingLineNumber(2)
            .fields().containsExactly(entry("head1", "foo"), entry("head2", "bar"));
    }

    @Test
    void stringArray() {
        final CsvCallbackHandler<String[]> rh = StringArrayHandler.of();
        process(rh);

        assertThat(rh.buildRecord())
            .containsExactly("foo", "bar");
    }

    @Test
    void stringArrayFieldModifier() {
        final CsvCallbackHandler<String[]> rh = StringArrayHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
        rh.beginRecord(1);
        addField(rh, " foo");
        addField(rh, "bar ");

        assertThat(rh.buildRecord())
            .containsExactly("foo", "bar");
    }

    private static void process(final CsvCallbackHandler<?> rh) {
        rh.beginRecord(1);
        addField(rh, "foo");
        addField(rh, "bar");
    }

    private static void addField(final CsvCallbackHandler<?> rh, final String value) {
        rh.addField(value.toCharArray(), 0, value.length(), false);
    }

}
