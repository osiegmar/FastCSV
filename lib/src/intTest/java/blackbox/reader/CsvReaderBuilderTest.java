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
        assertThatThrownBy(() -> crb.ofCsvRecord((String) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fieldSeparator() {
        final Iterator<CsvRecord> it = crb.fieldSeparator(';')
            .ofCsvRecord("foo,bar;baz").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_RECORD)
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo,bar", "baz");
    }

    @Test
    void quoteCharacter() {
        final Iterator<CsvRecord> it = crb.quoteCharacter('_')
            .ofCsvRecord("_foo \", __ bar_,foo \" bar").iterator();
        assertThat(it).toIterable()
            .singleElement(CSV_RECORD)
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo \", _ bar", "foo \" bar");
    }

    @Test
    void commentSkip() {
        final Iterator<CsvRecord> it = crb.commentCharacter(';').commentStrategy(CommentStrategy.SKIP)
            .ofCsvRecord("#foo\n;bar\nbaz").iterator();
        assertThat(it).toIterable()
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1)
                    .isStartingLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("#foo"),
                item2 -> CsvRecordAssert.assertThat(item2)
                    .isStartingLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz"));
    }

    @Test
    void builderToString() {
        assertThat(crb).asString()
            .isEqualTo("""
                CsvReaderBuilder[fieldSeparator=,, quoteCharacter=", \
                commentStrategy=NONE, commentCharacter=#, skipEmptyLines=true, \
                allowExtraFields=false, allowMissingFields=false, allowExtraCharsAfterClosingQuote=false, \
                trimWhitespacesAroundQuotes=false, detectBomHeader=false, maxBufferSize=16777216]""");
    }

    @Test
    void string() {
        assertThat(crb.ofCsvRecord(DATA).stream())
            .singleElement(CSV_RECORD)
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().isEqualTo(EXPECTED);
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.writeString(file, DATA);

        try (Stream<CsvRecord> stream = crb.ofCsvRecord(file).stream()) {
            assertThat(stream)
                .singleElement(CSV_RECORD)
                .isStartingLineNumber(1)
                .isNotComment()
                .fields().isEqualTo(EXPECTED);
        }
    }

    @Test
    void pathCharset(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.writeString(file, DATA);

        try (Stream<CsvRecord> stream = crb.ofCsvRecord(file, UTF_8).stream()) {
            assertThat(stream)
                .singleElement(CSV_RECORD)
                .isStartingLineNumber(1)
                .isNotComment()
                .fields().isEqualTo(EXPECTED);
        }
    }

    @Test
    void chained() {
        final CsvReader<CsvRecord> reader = CsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .skipEmptyLines(true)
            .allowExtraFields(false)
            .allowMissingFields(false)
            .allowExtraCharsAfterClosingQuote(false)
            .ofCsvRecord("foo");

        assertThat(reader).isNotNull();
    }

}
