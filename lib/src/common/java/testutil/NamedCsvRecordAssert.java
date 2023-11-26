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

    public NamedCsvRecordAssert isOriginalLineNumber(final long originalLineNumber) {
        isNotNull();
        if (actual.getOriginalLineNumber() != originalLineNumber) {
            failWithMessage("Expected original line number to be <%d> but was <%d>",
                originalLineNumber, actual.getOriginalLineNumber());
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

}
