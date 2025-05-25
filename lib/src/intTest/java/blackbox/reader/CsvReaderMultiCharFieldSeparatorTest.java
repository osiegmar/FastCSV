package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import testutil.CsvRecordAssert;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
class CsvReaderMultiCharFieldSeparatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    void nullAndEmptyFieldSeparator(final String fieldSeparator) {
        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(fieldSeparator).ofCsvRecord(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("fieldSeparator must not be null or empty");
    }

    @Test
    void trimAroundQuotes() {
        final var csv = CsvReader.builder()
            .trimWhitespacesAroundQuotes(true)
            .fieldSeparator("~~~")
            .quoteCharacter('\'')
            .ofCsvRecord("\t'foo~~~bar' ");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("foo~~~bar")
        );
    }

    @Test
    void notTrimAroundQuotes() {
        // Enforce the use of the RelaxedParser by using a multi-character field separator
        final var csv = CsvReader.builder()
            .fieldSeparator("~~~")
            .quoteCharacter('\'')
            .ofCsvRecord(" 'foo~~~bar' ");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly(" 'foo", "bar' ")
        );
    }

    @Test
    void charsBeforeQuotes() {
        final var csv = CsvReader.builder()
            .trimWhitespacesAroundQuotes(true)
            .quoteCharacter('\'')
            .ofCsvRecord("x'foo,bar'");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("x'foo", "bar'")
        );
    }

    @Test
    void charsAfterQuotes() {
        final var csv = CsvReader.builder()
            .trimWhitespacesAroundQuotes(true)
            .quoteCharacter('\'')
            .ofCsvRecord(" 'foo,bar'x ");

        assertThatThrownBy(() -> csv.stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Exception when reading first record")
            .hasRootCauseMessage("Unexpected character after closing quote: 'x' (0x78)");
    }

    @Test
    void multiCharFieldSeparator() {
        final var csv = CsvReader.builder()
            .fieldSeparator("~~~")
            .ofCsvRecord("foo~bar~~~baz");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("foo~bar", "baz")
        );
    }

    @Test
    void fieldSeparatorPartiallyOverlapping() {
        assertThatThrownBy(() -> CsvReader.builder()
            .quoteCharacter('~').fieldSeparator("~~").ofCsvRecord("foo~bar~~baz")
            .stream().count()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=~, quoteCharacter=~, commentCharacter=#)");
    }

    @Test
    void multiCharFieldSeparatorAfterQuote() {
        final var csv = CsvReader.builder()
            .fieldSeparator("~~~")
            .quoteCharacter('\'')
            .ofCsvRecord("'foo~~~bar'~~~baz");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("foo~~~bar", "baz")
        );
    }

    // The point of multi-char field separators is mainly to allow fully unquoted data
    @Test
    void unquotedData() {
        final var csv = CsvReader.builder()
            .fieldSeparator("||")
            .quoteCharacter('\'')
            .build(CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM)),
                "  my value  ||  my 'value'|2  ");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("my value", "my 'value'|2")
        );
    }

    @Test
    void allowExtraCharsAfterClosingQuoteOnlyInStrictMode() {
        assertThatThrownBy(() -> CsvReader.builder()
            .allowExtraCharsAfterClosingQuote(true).fieldSeparator(";;").ofCsvRecord("foo")
            .stream().count()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("allowExtraCharsAfterClosingQuote is not supported in relaxed mode");
    }

}
