package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvCallbackHandlers;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.CsvRecordAssert;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CsvCallbackHandlerTest {

    @Test
    void csvRecord() {
        final CsvCallbackHandler<CsvRecord> rh = CsvCallbackHandlers.ofCsvRecord();
        process(rh);

        CsvRecordAssert.assertThat(rh.buildRecord())
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

    @Test
    void namedCsvRecord() {
        final CsvCallbackHandler<NamedCsvRecord> rh = CsvCallbackHandlers.ofNamedCsvRecord();

        rh.beginRecord(1);
        rh.addField("head1", false);
        rh.addField("head2", false);
        rh.buildRecord();

        rh.beginRecord(2);
        rh.addField("foo", false);
        rh.addField("bar", false);

        NamedCsvRecordAssert.assertThat(rh.buildRecord())
            .isStartingLineNumber(2)
            .fields().containsExactly(entry("head1", "foo"), entry("head2", "bar"));
    }

    @Test
    void stringArray() {
        final CsvCallbackHandler<String[]> rh = CsvCallbackHandlers.ofStringArray();
        process(rh);

        assertThat(rh.buildRecord())
            .containsExactly("foo", "bar");
    }

    @Test
    void simpleMapper() {
        final CsvCallbackHandler<String[]> rh = CsvCallbackHandlers
            .forSimpleMapper(fields -> new String[]{fields[1], fields[0]});
        process(rh);

        assertThat(rh.buildRecord())
            .containsExactly("bar", "foo");
    }

    private static void process(final CsvCallbackHandler<?> rh) {
        rh.beginRecord(1);
        rh.addField("foo", false);
        rh.addField("bar", false);
    }

}
