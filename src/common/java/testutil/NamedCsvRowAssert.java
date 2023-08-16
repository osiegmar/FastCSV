package testutil;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.StringAssert;

import de.siegmar.fastcsv.reader.NamedCsvRow;

public class NamedCsvRowAssert extends AbstractAssert<NamedCsvRowAssert, NamedCsvRow> {

    public static final InstanceOfAssertFactory<NamedCsvRow, NamedCsvRowAssert> NAMED_CSV_ROW =
        new InstanceOfAssertFactory<>(NamedCsvRow.class, NamedCsvRowAssert::assertThat);

    protected NamedCsvRowAssert(final NamedCsvRow actual) {
        super(actual, NamedCsvRowAssert.class);
    }

    public static NamedCsvRowAssert assertThat(final NamedCsvRow actual) {
        return new NamedCsvRowAssert(actual);
    }

    public NamedCsvRowAssert isOriginalLineNumber(final long originalLineNumber) {
        isNotNull();
        if (actual.getOriginalLineNumber() != originalLineNumber) {
            failWithMessage("Expected original line number to be <%d> but was <%d>",
                originalLineNumber, actual.getOriginalLineNumber());
        }

        return this;
    }

    public MapAssert<String, String> fields() {
        isNotNull();
        return new MapAssert<>(actual.getFields());
    }

    public StringAssert field(final String name) {
        isNotNull();
        return new StringAssert(actual.getField(name));
    }

}
