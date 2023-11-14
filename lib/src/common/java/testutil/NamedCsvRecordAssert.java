package testutil;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
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
        if (actual.originalLineNumber() != originalLineNumber) {
            failWithMessage("Expected original line number to be <%d> but was <%d>",
                originalLineNumber, actual.originalLineNumber());
        }

        return this;
    }

    public MapAssert<String, String> fields() {
        isNotNull();
        return new MapAssert<>(actual.fieldsAsMap());
    }

    public StringAssert field(final String name) {
        isNotNull();
        return new StringAssert(actual.field(name));
    }

}
