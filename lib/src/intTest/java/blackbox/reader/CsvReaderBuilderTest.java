package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static testutil.CsvRecordAssert.CSV_RECORD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import testutil.CsvRecordAssert;

@SuppressWarnings("PMD.CloseResource")
class CsvReaderBuilderTest {

    private static final String DATA = "foo,bar\n";
    private static final List<String> EXPECTED = Arrays.asList("foo", "bar");

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @Test
    void nullInput() {
        assertThatThrownBy(() -> crb.build((String) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fieldSeparator() {
        final Iterator<CsvRecord> it = crb.fieldSeparator(';')
            .build("foo,bar;baz").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_RECORD)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo,bar", "baz");
    }

    @Test
    void quoteCharacter() {
        final Iterator<CsvRecord> it = crb.quoteCharacter('_')
            .build("_foo \", __ bar_,foo \" bar").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_RECORD)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo \", _ bar", "foo \" bar");
    }

    @Test
    void commentSkip() {
        final Iterator<CsvRecord> it = crb.commentCharacter(';').commentStrategy(CommentStrategy.SKIP)
            .build("#foo\n;bar\nbaz").iterator();
        assertThat(it).toIterable()
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("#foo"),
                item2 -> CsvRecordAssert.assertThat(item2)
                    .isOriginalLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz"));
    }

    @Test
    void builderToString() {
        assertThat(crb).asString()
            .isEqualTo("CsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
                + "commentStrategy=NONE, commentCharacter=#, skipEmptyLines=true, "
                + "ignoreDifferentFieldCount=true, fieldModifier=null]");
    }

    @Test
    void string() {
        assertThat(crb.build(DATA).stream())
            .singleElement(CSV_RECORD)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().isEqualTo(EXPECTED);
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        try (Stream<CsvRecord> stream = crb.build(file).stream()) {
            assertThat(stream)
                .singleElement(CSV_RECORD)
                .isOriginalLineNumber(1)
                .isNotComment()
                .fields().isEqualTo(EXPECTED);
        }
    }

    @Test
    void chained() {
        final CsvReader reader = CsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .skipEmptyLines(true)
            .ignoreDifferentFieldCount(false)
            .build("foo");

        assertThat(reader).isNotNull();
    }

}
