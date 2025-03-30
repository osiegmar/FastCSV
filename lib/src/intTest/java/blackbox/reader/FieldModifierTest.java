package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.FieldModifiers;
import testutil.CsvRecordAssert;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FieldModifierTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @Test
    void defaultImpl() {
        crb.commentStrategy(CommentStrategy.READ);

        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(new FieldModifier() { }));

        assertThat(crb.build(cbh, "foo\n#bar").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("foo"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly("bar")
            );
    }

    @Test
    void trim() {
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));

        assertThat(crb.acceptCharsAfterQuotes(true).build(cbh, "foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar\u2000", "baz");
    }

    @Test
    void strip() {
        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.STRIP));

        assertThat(crb.acceptCharsAfterQuotes(true).build(cbh, "foo, bar\u2000 ,\" baz \" ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar", "baz");
    }

    @Test
    void combination() {
        crb.commentStrategy(CommentStrategy.READ);

        final FieldModifier modifier = FieldModifiers.TRIM
            .andThen(FieldModifiers.modify(field -> "<" + field + ">"));

        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(modifier));

        assertThat(crb.build(cbh, "foo, bar , baz  \n# foo ").stream())
            .satisfiesExactly(
                item -> CsvRecordAssert.assertThat(item).fields().containsExactly("<foo>", "<bar>", "<baz>"),
                item -> CsvRecordAssert.assertThat(item).isComment().fields().containsExactly(" foo ")
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "#foo"})
    void noNull(final String value) {
        crb.commentStrategy(CommentStrategy.READ);

        final CsvRecordHandler cbh = CsvRecordHandler.of(c -> c.fieldModifier(new NullFieldModifier()));

        assertThatThrownBy(() -> crb.build(cbh, value).stream().toList())
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
