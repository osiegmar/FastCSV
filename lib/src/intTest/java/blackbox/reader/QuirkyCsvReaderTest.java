package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import testutil.CsvRecordAssert;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
class QuirkyCsvReaderTest {

    @Test
    void trimAroundQuotes() {
        final var csv = CsvReader.builder()
            .lenientSpacesAroundQuotes(true)
            .fieldSeparator("~~~")
            .quoteCharacter('\'')
            .ofCsvRecord(" 'foo~~~bar' ");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("foo~~~bar")
        );
    }

    @Test
    void notTrimAroundQuotes() {
        // Enforce the use of the LooseParser by using a multi-character field separator
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
            .lenientSpacesAroundQuotes(true)
            .quoteCharacter('\'')
            .ofCsvRecord("x'foo,bar'");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("x'foo", "bar'")
        );
    }

    @Test
    void charsAfterQuotes() {
        final var csv = CsvReader.builder()
            .lenientSpacesAroundQuotes(true)
            .quoteCharacter('\'')
            .ofCsvRecord(" 'foo,bar'x ");

        assertThatThrownBy(() -> csv.stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Exception when reading first record")
            .hasRootCauseMessage("Unexpected character after closing quote: 0x0078");
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
            .build(CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM)), "  my value  ||  my 'value'|2  ");

        assertThat(csv.stream()).satisfiesExactly(
            r -> CsvRecordAssert.assertThat(r).fields().containsExactly("my value", "my 'value'|2")
        );
    }

    @Test
    void acceptCharsAfterQuotesOnlyInStrictMode() {
        assertThatThrownBy(() -> CsvReader.builder()
            .acceptCharsAfterQuotes(true).fieldSeparator(";;").ofCsvRecord("foo")
            .stream().count()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("acceptCharsAfterQuotes is not supported in loose mode");
    }

}
