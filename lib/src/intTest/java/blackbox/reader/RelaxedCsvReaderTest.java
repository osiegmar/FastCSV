package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
class RelaxedCsvReaderTest extends AbstractCsvReaderTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("de.siegmar.fastcsv.relaxed", "true");
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("de.siegmar.fastcsv.relaxed");
    }

    @Test
    void allowExtraCharsAfterClosingQuote() {
        crb.allowExtraCharsAfterClosingQuote(true);
        assertThatThrownBy(() -> crb.ofCsvRecord("foo,\"bar\"baz").stream())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("allowExtraCharsAfterClosingQuote is not supported in relaxed mode");
    }

    // toString()

    @Test
    void readerToString() {
        assertThat(crb.ofCsvRecord(""))
            .asString()
            .isEqualTo("CsvReader[commentStrategy=NONE, skipEmptyLines=true, "
                + "ignoreDifferentFieldCount=true, parser=RelaxedCsvParser]");
    }

}
