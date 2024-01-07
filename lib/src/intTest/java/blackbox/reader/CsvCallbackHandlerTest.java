package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;
import de.siegmar.fastcsv.reader.StringArrayHandler;
import testutil.CsvRecordAssert;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CsvCallbackHandlerTest {

    @Test
    void csvRecord() {
        final CsvCallbackHandler<CsvRecord> rh = new CsvRecordHandler();
        process(rh);

        CsvRecordAssert.assertThat(rh.buildRecord().wrappedRecord())
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

    @Test
    void namedCsvRecord() {
        final CsvCallbackHandler<NamedCsvRecord> rh = new NamedCsvRecordHandler();

        rh.beginRecord(1);
        addField(rh, "head1");
        addField(rh, "head2");
        rh.buildRecord();

        rh.beginRecord(2);
        addField(rh, "foo");
        addField(rh, "bar");

        NamedCsvRecordAssert.assertThat(rh.buildRecord().wrappedRecord())
            .isStartingLineNumber(2)
            .fields().containsExactly(entry("head1", "foo"), entry("head2", "bar"));
    }

    @Test
    void stringArray() {
        final CsvCallbackHandler<String[]> rh = new StringArrayHandler();
        process(rh);

        assertThat(rh.buildRecord().wrappedRecord())
            .containsExactly("foo", "bar");
    }

    @Test
    void stringArrayFieldModifier() {
        final CsvCallbackHandler<String[]> rh = new StringArrayHandler(FieldModifiers.TRIM);
        rh.beginRecord(1);
        addField(rh, " foo");
        addField(rh, "bar ");

        assertThat(rh.buildRecord().wrappedRecord())
            .containsExactly("foo", "bar");
    }

    @Test
    void simpleMapper() {
        final CsvCallbackHandler<String[]> rh = CsvCallbackHandler
            .forSimpleMapper(fields -> new String[]{fields[1], fields[0]});
        process(rh);

        assertThat(rh.buildRecord().wrappedRecord())
            .containsExactly("bar", "foo");
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
