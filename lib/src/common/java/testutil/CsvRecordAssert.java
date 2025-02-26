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

    @SuppressWarnings("PMD.LinguisticNaming")
    public CsvRecordAssert isStartingLineNumber(final long startingLineNumber) {
        isNotNull();
        if (actual.getStartingLineNumber() != startingLineNumber) {
            failWithMessage("Expected starting line number to be <%d> but was <%d>",
                startingLineNumber, actual.getStartingLineNumber());
        }

        return this;
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public CsvRecordAssert isComment(final boolean comment) {
        isNotNull();
        if (actual.isComment() != comment) {
            failWithMessage("Expected comment to be <%b> but was <%b>",
                comment, actual.isComment());
        }

        return this;
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public CsvRecordAssert isComment() {
        return isComment(true);
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public CsvRecordAssert isNotComment() {
        return isComment(false);
    }

    public ListAssert<String> fields() {
        isNotNull();
        return new ListAssert<>(actual.getFields());
    }

}
