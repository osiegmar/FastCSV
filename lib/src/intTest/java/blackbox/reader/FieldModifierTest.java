package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.SimpleFieldModifier;
import testutil.CsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FieldModifierTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @Test
    void defaultImpl() {
        crb
            .commentStrategy(CommentStrategy.READ)
            .fieldModifier(new FieldModifier() {
            });

        assertThat(crb.ofCsvRecord("foo\n#bar").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("foo"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly("bar")
            );
    }

    @Test
    void trim() {
        crb.fieldModifier(FieldModifiers.TRIM);

        assertThat(crb.ofCsvRecord("foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar\u2000", "baz");
    }

    @Test
    void strip() {
        crb.fieldModifier(FieldModifiers.STRIP);

        assertThat(crb.ofCsvRecord("foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar", "baz");
    }

    @Test
    void combination() {
        final SimpleFieldModifier addSpaces = field -> " " + field + " ";

        crb
            .commentStrategy(CommentStrategy.READ)
            .fieldModifier(addSpaces
                .andThen(FieldModifiers.upper(Locale.ROOT))
                .andThen(FieldModifiers.lower(Locale.ROOT))
                .andThen(FieldModifiers.TRIM)
            );

        assertThat(crb.ofCsvRecord("FOO, bar , BAZ  \n# foo ").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("foo", "bar", "baz"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly(" foo ")
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "#foo"})
    void noNull(final String value) {
        crb
            .commentStrategy(CommentStrategy.READ)
            .fieldModifier(new NullFieldModifier());

        assertThatThrownBy(() -> crb.ofCsvRecord(value).stream().collect(Collectors.toList()))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading first record")
            .rootCause()
            .isInstanceOf(NullPointerException.class)
            .hasMessageStartingWith("Field modifier class blackbox.reader.FieldModifierTest")
            .hasMessageEndingWith("returned null for field 'foo' at field index 0 of line 1");
    }

    private static class NullFieldModifier implements FieldModifier {
        @Override
        public String modify(final long startingLineNumber, final int fieldIdx, final boolean quoted,
                             final String field) {
            return null;
        }

        @Override
        public String modifyComment(final long startingLineNumber, final String field) {
            return null;
        }
    }

}
