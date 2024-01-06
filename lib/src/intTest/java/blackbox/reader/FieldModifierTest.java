package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.SimpleFieldModifier;
import testutil.CsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FieldModifierTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @Test
    void defaultImpl() {
        crb.commentStrategy(CommentStrategy.READ);
        final FieldModifier modifier = new FieldModifier() { };

        assertThat(crb.build(new CsvRecordHandler(modifier), "foo\n#bar").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("foo"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly("bar")
            );
    }

    @Test
    void trim() {
        final FieldModifier modifier = FieldModifiers.TRIM;

        assertThat(crb.build(new CsvRecordHandler(modifier), "foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar\u2000", "baz");
    }

    @Test
    void strip() {
        final FieldModifier modifier = FieldModifiers.STRIP;

        assertThat(crb.build(new CsvRecordHandler(modifier), "foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar", "baz");
    }

    @Test
    void combination() {
        crb.commentStrategy(CommentStrategy.READ);

        final SimpleFieldModifier addSpaces = field -> " " + field + " ";
        final FieldModifier modifier = addSpaces
            .andThen(FieldModifiers.upper(Locale.ROOT))
            .andThen(FieldModifiers.lower(Locale.ROOT))
            .andThen(FieldModifiers.TRIM);

        assertThat(crb.build(new CsvRecordHandler(modifier), "FOO, bar , BAZ  \n# foo ").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("foo", "bar", "baz"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly(" foo ")
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "#foo"})
    void noNull(final String value) {
        crb.commentStrategy(CommentStrategy.READ);

        final FieldModifier modifier = new NullFieldModifier();

        assertThatThrownBy(() -> crb.build(new CsvRecordHandler(modifier), value).stream().toList())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading first record")
            .rootCause()
            .isInstanceOf(NullPointerException.class);
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
