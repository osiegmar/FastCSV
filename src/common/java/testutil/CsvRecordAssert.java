package testutil;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;

import de.siegmar.fastcsv.reader.CsvRecord;

public class CsvRecordAssert extends AbstractAssert<CsvRecordAssert, CsvRecord> {

    public static final InstanceOfAssertFactory<CsvRecord, CsvRecordAssert> CSV_RECORD =
        new InstanceOfAssertFactory<>(CsvRecord.class, CsvRecordAssert::assertThat);

    protected CsvRecordAssert(final CsvRecord actual) {
        super(actual, CsvRecordAssert.class);
    }

    public static CsvRecordAssert assertThat(final CsvRecord actual) {
        return new CsvRecordAssert(actual);
    }

    public CsvRecordAssert isOriginalLineNumber(final long originalLineNumber) {
        isNotNull();
        if (actual.originalLineNumber() != originalLineNumber) {
            failWithMessage("Expected original line number to be <%d> but was <%d>",
                originalLineNumber, actual.originalLineNumber());
        }

        return this;
    }

    public CsvRecordAssert isComment(final boolean comment) {
        isNotNull();
        if (actual.comment() != comment) {
            failWithMessage("Expected comment to be <%b> but was <%b>",
                comment, actual.comment());
        }

        return this;
    }

    public CsvRecordAssert isComment() {
        return isComment(true);
    }

    public CsvRecordAssert isNotComment() {
        return isComment(false);
    }

    public ListAssert<String> fields() {
        isNotNull();
        return new ListAssert<>(actual.fields());
    }

}
