package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static testutil.NamedCsvRecordAssert.NAMED_CSV_RECORD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.reader.MalformedCsvException;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings("PMD.CloseResource")
class NamedCsvReaderBuilderTest {

    private static final String DATA = "header1,header2\nfoo,bar\n";
    private static final Map<String, String> EXPECTED = Map.of(
        "header1", "foo",
        "header2", "bar");

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder();

    @Test
    void nullInput() {
        assertThatThrownBy(() -> crb.build((String) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fieldSeparator() {
        assertThat(crb.fieldSeparator(';').build("h1;h2\nfoo,bar;baz").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo,bar"), entry("h2", "baz"));
    }

    @Test
    void quoteCharacter() {
        assertThat(crb.quoteCharacter('_').build("h1,h2\n_foo \", __ bar_,foo \" bar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo \", _ bar"), entry("h2", "foo \" bar"));
    }

    @Test
    void commentSkip() {
        assertThat(crb.commentCharacter(';').skipComments(true).build("h1\n#foo\n;bar\nbaz").stream())
            .satisfiesExactly(
                item1 -> NamedCsvRecordAssert.assertThat(item1).fields().containsExactly(entry("h1", "#foo")),
                item2 -> NamedCsvRecordAssert.assertThat(item2).fields().containsExactly(entry("h1", "baz"))
            );
    }

    @Test
    void builderToString() {
        assertThat(crb).asString()
            .isEqualTo("NamedCsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
                + "commentCharacter=#, skipComments=false, ignoreDifferentFieldCount=true, fieldModifier=null]");
    }

    @Test
    void string() {
        assertThat(crb.build(DATA).stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactlyInAnyOrderEntriesOf(EXPECTED);
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        try (Stream<NamedCsvRecord> stream = crb.build(file).stream()) {
            assertThat(stream)
                .singleElement(NAMED_CSV_RECORD)
                .fields()
                .containsExactlyInAnyOrderEntriesOf(EXPECTED);
        }
    }

    @Test
    void chained() {
        final NamedCsvReader reader = NamedCsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentCharacter('#')
            .skipComments(false)
            .ignoreDifferentFieldCount(false)
            .build("foo");

        assertThat(reader).isNotNull();
    }

    @Test
    void differentFieldCountSuccess() {
        assertThat(crb.build("h1,h2,h3\nfoo,bar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .field("h2")
            .isEqualTo("bar");
    }

    @Test
    void differentFieldCountNullField() {
        assertThat(crb.build("h1,h2\nfoo").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"));
    }

    @Test
    void differentFieldCountFailInit() {
        crb.ignoreDifferentFieldCount(false);

        assertThatThrownBy(() -> crb.build("foo\nbar,\"baz\nbax\"").stream().findFirst())
            .isInstanceOf(MalformedCsvException.class)
            .hasMessage("Record 2 has 2 fields, but first record had 1 fields");
    }

    @Test
    void differentFieldCountFailAccess() {
        assertThatThrownBy(() -> crb.build("h1,h2,h3\nfoo,bar").stream()
            .findFirst().orElseThrow()
            .getField("h3"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Field 'h3' is on position 3, but current record only contains 2 fields");
    }

}
