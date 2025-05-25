package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
class NamedCsvReaderTest {

    @Test
    void simpleHeader() {
        final var reader = parse("foo\nbar").iterator();
        final NamedCsvRecord record = reader.next();
        NamedCsvRecordAssert.assertThat(record)
            .satisfies(
                r -> assertThat(r.getHeader()).containsExactly("foo"),
                r -> assertThat(r.getFieldsAsMap()).containsExactly(entry("foo", "bar")
                ));
    }

    @Test
    void fieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .field("foo").isEqualTo("bar");
    }

    @Test
    void findFieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .findField("foo").hasValue("bar");
    }

    @Test
    void findFieldsByName() {
        final var cbh = NamedCsvRecordHandler
            .of(c -> c.allowDuplicateHeader(true));
        assertThat(CsvReader.builder().build(cbh, "foo,xoo,foo\nbar,moo,baz").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .findFields("foo").containsExactly("bar", "baz");
    }

    @Test
    void nonExistingFieldName() {
        assertThatThrownBy(() -> parse("foo\nfaz").iterator().next().getField("bar"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Header does not contain a field 'bar'. Valid names are: [foo]");
    }

    @Test
    void nonExistingFieldName2() {
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
    void headerToString() {
        assertThat(parse("headerA,headerB,headerC\nfieldA,fieldB,fieldC\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("NamedCsvRecord[startingLineNumber=2, "
                + "fields=[fieldA, fieldB, fieldC], "
                + "comment=false, "
                + "header=[headerA, headerB, headerC]]");
    }

    @Test
    void fieldMap() {
        final var cbh = NamedCsvRecordHandler
            .of(c -> c.allowDuplicateHeader(true));
        assertThat(CsvReader.builder().build(cbh, "headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("headerA", "fieldA"), entry("headerB", "fieldB"));
    }

    @Test
    void allFieldsMap() {
        final var cbh = NamedCsvRecordHandler
            .of(c -> c.allowDuplicateHeader(true));
        assertThat(CsvReader.builder().build(cbh, "headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .allFields()
            .containsOnly(entry("headerA", List.of("fieldA", "fieldC")), entry("headerB", List.of("fieldB")));
    }

    @Test
    void customHeader() {
        final var cbh = NamedCsvRecordHandler.of(c -> c.header("h1", "h2"));
        assertThat(CsvReader.builder().build(cbh, "foo,bar").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    @Test
    void customHeaderList() {
        final var cbh = NamedCsvRecordHandler.of(c -> c.header(List.of("h1", "h2")));
        assertThat(CsvReader.builder().build(cbh, "foo,bar").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    @Test
    void customHeader2() {
        final var cbh = NamedCsvRecordHandler.of(c -> c.header("h1", "h2"));
        assertThat(CsvReader.builder().build(cbh, "foo,bar").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    // comments

    @Test
    void commentStrategyNone() {
        final var csvReader = CsvReader.builder()
            .ofNamedCsvRecord("#foo\nbar\n123");
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
            .ofNamedCsvRecord("#foo\nbar\n123");
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("bar", "123"))
            );
    }

    @Test
    void commentStrategyRead() {
        final var csvReader = CsvReader.builder()
            .commentStrategy(CommentStrategy.READ)
            .ofNamedCsvRecord("#comment1\nhead1\n#comment2\nvalue1");
        assertThat(csvReader.stream())
            .satisfiesExactly(
                c -> NamedCsvRecordAssert.assertThat(c).isComment().field(0).isEqualTo("comment1"),
                c -> NamedCsvRecordAssert.assertThat(c).isComment().field(0).isEqualTo("comment2"),
                c -> NamedCsvRecordAssert.assertThat(c).fields().containsExactly(entry("head1", "value1"))
            );
    }

    // Builder methods

    @Test
    void string() {
        assertThat(CsvReader.builder().ofNamedCsvRecord("h1,h2\nv1,v2").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .isStartingLineNumber(2)
            .isNotComment()
            .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
    }

    @Test
    void reader() {
        assertThat(CsvReader.builder().ofNamedCsvRecord(new StringReader("h1,h2\nv1,v2")).stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .isStartingLineNumber(2)
            .isNotComment()
            .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.writeString(file, "h1,h2\nv1,v2");

        try (Stream<NamedCsvRecord> stream = CsvReader.builder().ofNamedCsvRecord(file).stream()) {
            assertThat(stream)
                .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
                .isStartingLineNumber(2)
                .isNotComment()
                .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
        }
    }

    @Test
    void pathCharset(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.writeString(file, "h1,h2\nv1,v2");

        try (Stream<NamedCsvRecord> stream =
                 CsvReader.builder().ofNamedCsvRecord(file, StandardCharsets.UTF_8).stream()) {
            assertThat(stream)
                .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
                .isStartingLineNumber(2)
                .isNotComment()
                .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
        }
    }

    // With field modifier

    @Test
    void fieldModifier() {
        final NamedCsvRecordHandler cbh = NamedCsvRecordHandler.of(c -> c.fieldModifier(FieldModifiers.TRIM));
        assertThat(CsvReader.builder().build(cbh, "h1 , h2 \n v1 , v2").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .isStartingLineNumber(2)
            .isNotComment()
            .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
    }

    @Test
    void fieldModifierPredefinedHeader() {
        final NamedCsvRecordHandler cbh = NamedCsvRecordHandler.of(c -> c
            .fieldModifier(FieldModifiers.TRIM)
            .header("h1", "h2"));
        assertThat(CsvReader.builder().build(cbh, "v1 , v2").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
    }

    @Test
    void fieldModifierPredefinedHeaderList() {
        final NamedCsvRecordHandler cbh = NamedCsvRecordHandler.of(c -> c
            .fieldModifier(FieldModifiers.TRIM)
            .header("h1", "h2"));
        assertThat(CsvReader.builder().build(cbh, "v1 , v2").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly(entry("h1", "v1"), entry("h2", "v2"));
    }

    // test helpers

    private CsvReader<NamedCsvRecord> parse(final String data) {
        return CsvReader.builder().ofNamedCsvRecord(data);
    }

}
