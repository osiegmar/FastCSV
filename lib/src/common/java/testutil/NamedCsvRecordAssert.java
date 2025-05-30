package testutil;

import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.OptionalAssert;
import org.assertj.core.api.StringAssert;

import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class NamedCsvRecordAssert extends AbstractAssert<NamedCsvRecordAssert, NamedCsvRecord> {

    public static final InstanceOfAssertFactory<NamedCsvRecord, NamedCsvRecordAssert> NAMED_CSV_RECORD =
        new InstanceOfAssertFactory<>(NamedCsvRecord.class, NamedCsvRecordAssert::assertThat);

    protected NamedCsvRecordAssert(final NamedCsvRecord actual) {
        super(actual, NamedCsvRecordAssert.class);
    }

    public static NamedCsvRecordAssert assertThat(final NamedCsvRecord actual) {
        return new NamedCsvRecordAssert(actual);
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public NamedCsvRecordAssert isStartingLineNumber(final long startingLineNumber) {
        isNotNull();
        if (actual.getStartingLineNumber() != startingLineNumber) {
            failWithMessage("Expected starting line number to be <%d> but was <%d>",
                startingLineNumber, actual.getStartingLineNumber());
        }

        return this;
    }

    public MapAssert<String, String> fields() {
        isNotNull();
        return new MapAssert<>(actual.getFieldsAsMap());
    }

    public MapAssert<String, List<String>> allFields() {
        isNotNull();
        return new MapAssert<>(actual.getFieldsAsMapList());
    }

    public StringAssert field(final int index) {
        isNotNull();
        return new StringAssert(actual.getField(index));
    }

    public StringAssert field(final String name) {
        isNotNull();
        return new StringAssert(actual.getField(name));
    }

    public OptionalAssert<String> findField(final String name) {
        isNotNull();
        return Assertions.assertThat(actual.findField(name));
    }

    public ListAssert<String> findFields(final String name) {
        isNotNull();
        return Assertions.assertThat(actual.findFields(name));
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public NamedCsvRecordAssert isComment(final boolean comment) {
        isNotNull();
        if (actual.isComment() != comment) {
            failWithMessage("Expected comment to be <%b> but was <%b>",
                comment, actual.isComment());
        }

        return this;
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public NamedCsvRecordAssert isComment() {
        return isComment(true);
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    public NamedCsvRecordAssert isNotComment() {
        return isComment(false);
    }

    public ListAssert<String> header() {
        isNotNull();
        return Assertions.assertThat(actual.getHeader());
    }

}
