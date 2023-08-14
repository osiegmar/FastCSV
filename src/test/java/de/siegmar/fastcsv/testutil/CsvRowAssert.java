package de.siegmar.fastcsv.testutil;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import de.siegmar.fastcsv.reader.CsvRow;

public class CsvRowAssert extends AbstractAssert<CsvRowAssert, CsvRow> {

    protected CsvRowAssert(final CsvRow actual) {
        super(actual, CsvRowAssert.class);
    }

    public static CsvRowAssert assertThat(final CsvRow actual) {
        return new CsvRowAssert(actual);
    }

    public CsvRowAssert isOriginalLineNumber(final long originalLineNumber) {
        isNotNull();
        if (actual.getOriginalLineNumber() != originalLineNumber) {
            failWithMessage("Expected original line number to be <%d> but was <%d>",
                originalLineNumber, actual.getOriginalLineNumber());
        }

        return this;
    }

    public CsvRowAssert isComment(final boolean comment) {
        isNotNull();
        if (actual.isComment() != comment) {
            failWithMessage("Expected comment to be <%b> but was <%b>",
                comment, actual.isComment());
        }

        return this;
    }

    public CsvRowAssert isComment() {
        return isComment(true);
    }

    public CsvRowAssert isNotComment() {
        return isComment(false);
    }

    public ListAssert<String> fields() {
        isNotNull();
        return new ListAssert<>(actual.getFields());
    }

}
