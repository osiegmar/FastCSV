package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CountingStatusListener;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import testutil.CsvRowAssert;

@ExtendWith(SoftAssertionsExtension.class)
class IndexedCsvReaderTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    private static final InstanceOfAssertFactory<CsvRow, CsvRowAssert> CSV_ROW =
        new InstanceOfAssertFactory<>(CsvRow.class, CsvRowAssert::assertThat);
    private static final String TEST_STRING = "foo";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @TempDir
    private Path tmpDir;

    @Test
    void outOfBounds() throws IOException {
        try (IndexedCsvReader reader = build("")) {
            softly.assertThat(reader.size())
                .succeedsWithin(TIMEOUT)
                .isEqualTo(0);

            softly.assertThat(reader.readRow(0))
                .failsWithin(TIMEOUT)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IndexOutOfBoundsException.class);

            softly.assertThat(reader.readRow(1))
                .failsWithin(TIMEOUT)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Test
    void readerToString() throws IOException {
        final Path file = prepareTestFile(TEST_STRING);

        assertThat(builder().build(file)).asString()
            .isEqualTo("IndexedCsvReader[file=%s, charset=%s, fieldSeparator=%s, "
                    + "quoteCharacter=%s, commentStrategy=%s, commentCharacter=%s]",
                file, UTF_8, ',', '"', CommentStrategy.NONE, '#');
    }

    // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
    @Test
    void unicode() throws IOException {
        final Path file = prepareTestFile("abc\nüöä\nabc");
        assertThat(builder().build(file).readRow(0))
            .succeedsWithin(TIMEOUT, CSV_ROW)
            .fields().singleElement().isEqualTo("abc");

        assertThat(builder().build(file).readRow(1))
            .succeedsWithin(TIMEOUT, CSV_ROW)
            .fields().singleElement().isEqualTo("üöä");

        assertThat(builder().build(file).readRow(2))
            .succeedsWithin(TIMEOUT, CSV_ROW)
            .fields().singleElement().isEqualTo("abc");
    }

    private IndexedCsvReader build(final String data) throws IOException {
        return builder().build(prepareTestFile(data));
    }

    private static IndexedCsvReader.IndexedCsvReaderBuilder builder() {
        return IndexedCsvReader.builder();
    }

    private Path prepareTestFile(final String s) throws IOException {
        return prepareTestFile(s.getBytes(UTF_8));
    }

    private Path prepareTestFile(final byte[] data) throws IOException {
        final Path file = tmpDir.resolve("foo.csv");
        Files.write(file, data, StandardOpenOption.CREATE_NEW);
        return file;
    }

    @Nested
    class IllegalInput {

        @ParameterizedTest
        @ValueSource(chars = {'\r', '\n'})
        void controlCharacterNewline(final char c) {
            final String expectedMessage = "A newline character must not be used as control character";

            softly.assertThatThrownBy(() -> builder().fieldSeparator(c))
                .as("fieldSeparator=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> builder().quoteCharacter(c))
                .as("quoteCharacter=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> builder().commentCharacter(c))
                .as("commentCharacter=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void controlCharacterMultibyte() {
            final String expectedMessage =
                "Multibyte control characters are not supported in IndexedCsvReader: '' (value: 128)";

            softly.assertThatThrownBy(() -> builder().fieldSeparator('\u0080'))
                .as("fieldSeparator")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> builder().quoteCharacter('\u0080'))
                .as("quoteCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> builder().commentCharacter('\u0080'))
                .as("commentCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void controlCharacterDiffer() throws IOException {
            final Path emptyFile = Files.createTempFile(tmpDir, "fastcsv", null);

            final String expectedMessage =
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)";

            softly.assertThatThrownBy(() -> builder().fieldSeparator('"').build(emptyFile))
                .as("fieldSeparator")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, "\"", "\"", "#");

            softly.assertThatThrownBy(() -> builder().quoteCharacter(',').build(emptyFile))
                .as("quoteCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", ",", "#");

            softly.assertThatThrownBy(() -> builder().commentCharacter(',').build(emptyFile))
                .as("commentCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", "\"", ",");
        }

        @Test
        void nullFile() {
            assertThatThrownBy(() -> builder().build(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("file must not be null");
        }

        @Test
        void negativePosition() {
            assertThatThrownBy(() -> build(TEST_STRING).readRow(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Row# must be >= 0");
        }

        @Test
        void nullCharset() {
            assertThatThrownBy(() -> builder().build(Paths.get("/tmp"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("charset must not be null");
        }

    }

    @Nested
    class Status {

        @Test
        void await() {
            assertThatCode(() -> build(TEST_STRING).completableFuture().get())
                .doesNotThrowAnyException();
        }

        @Test
        void finalStatus() throws IOException {
            final var statusListener = new CountingStatusListener() {

                public int getRowCount() {
                    return rowCnt.intValue();
                }

                public long getBytesRead() {
                    return byteCnt.longValue();
                }

            };

            final IndexedCsvReader reader = builder()
                .statusListener(statusListener)
                .build(prepareTestFile("foo\nbar"));

            try (reader) {
                assertThat(reader.completableFuture()).succeedsWithin(TIMEOUT);
                assertThat(statusListener.getRowCount()).isEqualTo(2);
                assertThat(statusListener.getBytesRead()).isEqualTo(7);
                assertThat(statusListener).asString()
                    .isEqualTo("Read %,d rows and %,d of %,d bytes (%.2f %%)", 2, 7, 7, 100.0);
            }
        }

    }

    @Nested
    class SingleRead {

        // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
        @Test
        void oneLine() throws IOException {
            try (IndexedCsvReader reader = build("012")) {
                assertThat(reader.size())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(1);

                assertThat(reader.readRow(0))
                    .succeedsWithin(TIMEOUT, CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012");
            }
        }

        // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
        @Test
        void twoLines() throws IOException {
            try (IndexedCsvReader reader = build("012,foo\n345,bar")) {

                assertThat(reader.size())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(2);

                assertThat(reader.readRow(0))
                    .succeedsWithin(TIMEOUT, CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012", TEST_STRING);

                assertThat(reader.readRow(1))
                    .succeedsWithin(TIMEOUT, CSV_ROW)
                    .isOriginalLineNumber(2)
                    .isNotComment()
                    .fields().containsExactly("345", "bar");
            }
        }

    }

    @Nested
    class Multilines {

        @Test
        void start0EndInfinite() throws IOException, ExecutionException, InterruptedException, TimeoutException {
            assertThat(readRows(0, Integer.MAX_VALUE))
                .flatMap(CsvRow::getFields)
                .containsExactly("1", "2", "3", "4", "5");
        }

        @Test
        void start1End2() throws IOException, ExecutionException, InterruptedException, TimeoutException {
            assertThat(readRows(1, 2))
                .flatMap(CsvRow::getFields)
                .containsExactly("2", "3");
        }

        private List<CsvRow> readRows(final int firstRecord, final int maxRecords)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {

            return build("1\n2\n3\n4\n5")
                .readRows(firstRecord, maxRecords)
                .get(1, TimeUnit.SECONDS)
                .collect(Collectors.toList());
        }

    }

}
