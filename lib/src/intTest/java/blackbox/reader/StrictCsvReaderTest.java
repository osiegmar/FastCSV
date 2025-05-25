package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import testutil.CsvRecordAssert;

class StrictCsvReaderTest extends AbstractCsvReaderTest {

    @Test
    void allowExtraCharsAfterClosingQuote() {
        crb.allowExtraCharsAfterClosingQuote(true);
        assertThat(crb.ofCsvRecord("foo,\"bar\"baz").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields().containsExactly("foo", "barbaz");
    }

    // toString()

    @Test
    void readerToString() {
        assertThat(crb.ofCsvRecord(""))
            .asString()
            .isEqualTo("CsvReader[commentStrategy=NONE, skipEmptyLines=true, "
                + "allowExtraFields=false, allowMissingFields=false, parser=StrictCsvParser]");
    }

}
