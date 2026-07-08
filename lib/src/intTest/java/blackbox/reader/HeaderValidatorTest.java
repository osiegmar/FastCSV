package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.HeaderValidator;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class HeaderValidatorTest {

    @Test
    void containsExactlyAccepts() {
        assertThatCode(() -> HeaderValidator.containsExactly("col1", "col2")
            .validate(List.of("col1", "col2")))
            .doesNotThrowAnyException();
    }

    @Test
    void containsExactlyRejectsDifferentFields() {
        assertThatThrownBy(() -> HeaderValidator.containsExactly(List.of("col1", "col2"))
            .validate(List.of("colA", "colB")))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Header mismatch: expected [col1, col2] but found [colA, colB]");
    }

    @Test
    void containsExactlyRejectsDifferentOrder() {
        assertThatThrownBy(() -> HeaderValidator.containsExactly("col1", "col2")
            .validate(List.of("col2", "col1")))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Header mismatch: expected [col1, col2] but found [col2, col1]");
    }

    @Test
    void containsExactlyRejectsExtraFields() {
        assertThatThrownBy(() -> HeaderValidator.containsExactly("col1", "col2")
            .validate(List.of("col1", "col2", "col3")))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Header mismatch: expected [col1, col2] but found [col1, col2, col3]");
    }

    @Test
    void containsExactlyNull() {
        assertThatThrownBy(() -> HeaderValidator.containsExactly((String[]) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("expectedHeader must not be null");
        assertThatThrownBy(() -> HeaderValidator.containsExactly((List<String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("expectedHeader must not be null");
        assertThatThrownBy(() -> HeaderValidator.containsExactly("col1", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void containsAtLeastAccepts() {
        assertThatCode(() -> HeaderValidator.containsAtLeast("col2")
            .validate(List.of("col1", "col2")))
            .doesNotThrowAnyException();
    }

    @Test
    void containsAtLeastAcceptsAnyOrder() {
        assertThatCode(() -> HeaderValidator.containsAtLeast(List.of("col2", "col1"))
            .validate(List.of("col1", "col2", "col3")))
            .doesNotThrowAnyException();
    }

    @Test
    void containsAtLeastRejectsMissingFields() {
        assertThatThrownBy(() -> HeaderValidator.containsAtLeast("col1", "col2", "col3")
            .validate(List.of("col1", "colA")))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Header is missing fields [col2, col3]: [col1, colA]");
    }

    @Test
    void containsAtLeastNull() {
        assertThatThrownBy(() -> HeaderValidator.containsAtLeast((String[]) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requiredFields must not be null");
        assertThatThrownBy(() -> HeaderValidator.containsAtLeast((List<String>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("requiredFields must not be null");
        assertThatThrownBy(() -> HeaderValidator.containsAtLeast("col1", null))
            .isInstanceOf(NullPointerException.class);
    }

}
