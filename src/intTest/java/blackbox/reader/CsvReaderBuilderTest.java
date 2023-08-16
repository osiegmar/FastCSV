package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static testutil.CsvRowAssert.CSV_ROW;

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
import de.siegmar.fastcsv.reader.CsvRow;
import testutil.CsvRowAssert;

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
        final Iterator<CsvRow> it = crb.fieldSeparator(';')
            .build("foo,bar;baz").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_ROW)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo,bar", "baz");
    }

    @Test
    void quoteCharacter() {
        final Iterator<CsvRow> it = crb.quoteCharacter('_')
            .build("_foo \", __ bar_,foo \" bar").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_ROW)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo \", _ bar", "foo \" bar");
    }

    @Test
    void commentSkip() {
        final Iterator<CsvRow> it = crb.commentCharacter(';').commentStrategy(CommentStrategy.SKIP)
            .build("#foo\n;bar\nbaz").iterator();
        assertThat(it).toIterable()
            .satisfiesExactly(
                item1 -> CsvRowAssert.assertThat(item1)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("#foo"),
                item2 -> CsvRowAssert.assertThat(item2)
                    .isOriginalLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz"));
    }

    @Test
    void builderToString() {
        assertThat(crb).asString()
            .isEqualTo("CsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
                + "commentStrategy=NONE, commentCharacter=#, skipEmptyRows=true, "
                + "errorOnDifferentFieldCount=false]");
    }

    @Test
    void string() {
        assertThat(crb.build(DATA).stream())
            .singleElement(CSV_ROW)
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().isEqualTo(EXPECTED);
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        try (Stream<CsvRow> stream = crb.build(file).stream()) {
            assertThat(stream)
                .singleElement(CSV_ROW)
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
            .skipEmptyRows(true)
            .errorOnDifferentFieldCount(false)
            .build("foo");

        assertThat(reader).isNotNull();
    }

}
