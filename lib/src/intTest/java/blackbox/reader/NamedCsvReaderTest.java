package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static testutil.NamedCsvRecordAssert.NAMED_CSV_RECORD;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvCallbackHandlers;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
class NamedCsvReaderTest {

    @Test
    void getHeader() {
        final var reader = parse("foo\nbar").iterator();
        final NamedCsvRecord record = reader.next();
        NamedCsvRecordAssert.assertThat(record)
            .satisfies(
                r -> assertThat(r.getHeader()).containsExactly("foo"),
                r -> assertThat(r.getFieldsAsMap()).containsExactly(entry("foo", "bar")
                ));
    }

    @Test
    void getFieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .field("foo").isEqualTo("bar");
    }

    @Test
    void findFieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .findField("foo").hasValue("bar");
    }

    @Test
    void findFieldsByName() {
        assertThat(parse("foo,xoo,foo\nbar,moo,baz").stream())
            .singleElement(NAMED_CSV_RECORD)
            .findFields("foo").containsExactly("bar", "baz");
    }

    @Test
    void getNonExistingFieldByName() {
        assertThatThrownBy(() -> parse("foo\nfaz").iterator().next().getField("bar"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Header does not contain a field 'bar'. Valid names are: [foo]");
    }

    @Test
    void getNonExistingFieldByName2() {
        assertThatThrownBy(() -> parse("foo,bar\nfaz").iterator().next().getField("bar"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Field 'bar' is on index 1, but current record only contains 1 fields");
    }

    @Test
    void findNonExistingFieldByName() {
        assertThat(parse("foo\nfaz").iterator().next().findField("bar"))
            .isEmpty();
    }

    @Test
    void findNonExistingFieldByName2() {
        assertThat(parse("foo,bar\nfaz").iterator().next().findField("bar"))
            .isEmpty();
    }

    @Test
    void toStringWithHeader() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("NamedCsvRecord[startingLineNumber=2, "
                + "fields=[fieldA, fieldB, fieldC], "
                + "comment=false, "
                + "header=[headerA, headerB, headerA]]");
    }

    @Test
    void fieldMap() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("headerA", "fieldA"), entry("headerB", "fieldB"));
    }

    @Test
    void allFieldsMap() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NAMED_CSV_RECORD)
            .allFields()
            .containsOnly(entry("headerA", List.of("fieldA", "fieldC")), entry("headerB", List.of("fieldB")));
    }

    @Test
    void customHeader() {
        final var recordHandler = CsvCallbackHandlers.ofNamedCsvRecord("h1", "h2");
        final var csvReader = CsvReader.builder().build("foo,bar", recordHandler);
        assertThat(csvReader.stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    @Test
    void customHeader2() {
        final var recordHandler = CsvCallbackHandlers.ofNamedCsvRecord(List.of("h1", "h2"));
        final var csvReader = CsvReader.builder().build("foo,bar", recordHandler);
        assertThat(csvReader.stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    // comments

    @Test
    void commentStrategyNone() {
        final var csvReader = CsvReader.builder()
            .build("#foo\nbar\n123", CsvCallbackHandlers.ofNamedCsvRecord());
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("#foo", "bar")),
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("#foo", "123"))
            );
    }

    @Test
    void commentStrategySkip() {
        final var csvReader = CsvReader.builder()
            .commentStrategy(CommentStrategy.SKIP)
            .build("#foo\nbar\n123", CsvCallbackHandlers.ofNamedCsvRecord());
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("bar", "123"))
            );
    }

    @Test
    void commentStrategyRead() {
        final var csvReader = CsvReader.builder()
            .commentStrategy(CommentStrategy.READ)
            .build("#comment1\nhead1\n#comment2\nvalue1", CsvCallbackHandlers.ofNamedCsvRecord());
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> NamedCsvRecordAssert.assertThat(c).isComment().field(0).isEqualTo("comment1"),
                c -> NamedCsvRecordAssert.assertThat(c).isComment().field(0).isEqualTo("comment2"),
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("head1", "value1"))
            );
    }

    // test helpers

    private CsvReader<NamedCsvRecord> parse(final String data) {
        return CsvReader.builder().build(data, CsvCallbackHandlers.ofNamedCsvRecord());
    }

}
