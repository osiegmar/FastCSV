package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static testutil.CsvRowAssert.CSV_PAGE;
import static testutil.CsvRowAssert.CSV_ROW;

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

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CollectingStatusListener;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import testutil.CsvRowAssert;

@ExtendWith(SoftAssertionsExtension.class)
class IndexedCsvReaderTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private static final String TEST_STRING = "foo";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @TempDir
    private Path tmpDir;

    @Test
    void outOfBounds() throws IOException {
        try (var csv = buildSinglePage("")) {
            softly.assertThat(csv.pageCount())
                .succeedsWithin(TIMEOUT)
                .isEqualTo(0);

            softly.assertThat(csv.rowCount())
                .succeedsWithin(TIMEOUT)
                .isEqualTo(0L);

            softly.assertThat(csv.readPage(0))
                .failsWithin(TIMEOUT)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IndexOutOfBoundsException.class)
                .withMessage("java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0");
        }
    }

    @Test
    void readerToString() throws IOException {
        final Path file = prepareTestFile(TEST_STRING);

        assertThat(singlePageBuilder().build(file)).asString()
            .isEqualTo("IndexedCsvReader[file=%s, charset=%s, fieldSeparator=%s, "
                    + "quoteCharacter=%s, commentStrategy=%s, commentCharacter=%s, pageSize=%d]",
                file, UTF_8, ',', '"', CommentStrategy.NONE, '#', 1);
    }

    // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
    @Test
    void unicode() throws IOException {
        try (var csv = buildSinglePage("abc\nüöä\nabc")) {
            assertThat(csv.readPage(0))
                .succeedsWithin(TIMEOUT, CSV_PAGE)
                .singleElement(CSV_ROW)
                .fields().singleElement().isEqualTo("abc");

            assertThat(csv.readPage(1))
                .succeedsWithin(TIMEOUT, CSV_PAGE)
                .singleElement(CSV_ROW)
                .fields().singleElement().isEqualTo("üöä");

            assertThat(csv.readPage(2))
                .succeedsWithin(TIMEOUT, CSV_PAGE)
                .singleElement(CSV_ROW)
                .fields().singleElement().isEqualTo("abc");
        }
    }

    private IndexedCsvReader buildSinglePage(final String data) throws IOException {
        return singlePageBuilder().build(prepareTestFile(data));
    }

    private static IndexedCsvReader.IndexedCsvReaderBuilder singlePageBuilder() {
        return IndexedCsvReader.builder().pageSize(1);
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

            softly.assertThatThrownBy(() -> singlePageBuilder().fieldSeparator(c))
                .as("fieldSeparator=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> singlePageBuilder().quoteCharacter(c))
                .as("quoteCharacter=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> singlePageBuilder().commentCharacter(c))
                .as("commentCharacter=%h", c)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void controlCharacterMultibyte() {
            final String expectedMessage =
                "Multibyte control characters are not supported in IndexedCsvReader: '' (value: 128)";

            softly.assertThatThrownBy(() -> singlePageBuilder().fieldSeparator('\u0080'))
                .as("fieldSeparator")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> singlePageBuilder().quoteCharacter('\u0080'))
                .as("quoteCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

            softly.assertThatThrownBy(() -> singlePageBuilder().commentCharacter('\u0080'))
                .as("commentCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        }

        @Test
        void controlCharacterDiffer() throws IOException {
            final Path emptyFile = Files.createTempFile(tmpDir, "fastcsv", null);

            final String expectedMessage =
                "Control characters must differ (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)";

            softly.assertThatThrownBy(() -> singlePageBuilder().fieldSeparator('"').build(emptyFile))
                .as("fieldSeparator")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, "\"", "\"", "#");

            softly.assertThatThrownBy(() -> singlePageBuilder().quoteCharacter('#').build(emptyFile))
                .as("quoteCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", "#", "#");

            softly.assertThatThrownBy(() -> singlePageBuilder().commentCharacter(',').build(emptyFile))
                .as("commentCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", "\"", ",");
        }

        @Test
        void nullFile() {
            assertThatThrownBy(() -> singlePageBuilder().build(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("file must not be null");
        }

        @Test
        void illegalPage() {
            assertThatThrownBy(() -> buildSinglePage(TEST_STRING).readPage(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be >= 0");
        }

        @Test
        void pageOutOfBounds() throws IOException {
            assertThat(buildSinglePage(TEST_STRING).readPage(10))
                .failsWithin(TIMEOUT)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IndexOutOfBoundsException.class)
                .withMessage("java.lang.IndexOutOfBoundsException: Index 10 out of bounds for length 1");
        }

        @Test
        void nullCharset() {
            assertThatThrownBy(() -> singlePageBuilder().build(Paths.get("/tmp"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("charset must not be null");
        }

        @Test
        void zeroPageSize() {
            assertThatThrownBy(() -> singlePageBuilder().pageSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be > 0");
        }

    }

    @Nested
    class Comment {

        @Test
        void readComment() throws IOException {
            final Path file = prepareTestFile("foo\n#a,b,c\nbaz");

            final var csv = singlePageBuilder()
                .commentStrategy(CommentStrategy.READ)
                .build(file);

            try (csv) {
                assertThat(csv.readPage(0))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("foo");

                assertThat(csv.readPage(1))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(2)
                    .isComment()
                    .fields().containsExactly("a,b,c");

                assertThat(csv.readPage(2))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz");
            }
        }

        @Test
        void noneComment() throws IOException {
            final Path file = prepareTestFile("foo\n#a,b,c\nbaz");

            final IndexedCsvReader csv = singlePageBuilder()
                .commentStrategy(CommentStrategy.NONE)
                .build(file);

            try (csv) {
                assertThat(csv.readPage(0))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("foo");

                assertThat(csv.readPage(1))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(2)
                    .isNotComment()
                    .fields().containsExactly("#a", "b", "c");

                assertThat(csv.readPage(2))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz");
            }
        }

        @Test
        void skipComment() {
            assertThatThrownBy(() -> singlePageBuilder().commentStrategy(CommentStrategy.SKIP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CommentStrategy SKIP is not supported in IndexedCsvReader");
        }

    }

    @Nested
    class Status {

        @Test
        void await() {
            assertThatCode(() -> buildSinglePage(TEST_STRING).completableFuture().get())
                .doesNotThrowAnyException();
        }

        @Test
        void finalStatus() throws IOException {
            final var statusListener = new CollectingStatusListener();

            final IndexedCsvReader csv = singlePageBuilder()
                .statusListener(statusListener)
                .build(prepareTestFile("foo\nbar"));

            try (csv) {
                assertThat(csv.completableFuture())
                    .succeedsWithin(TIMEOUT);

                assertThat(statusListener.getFileSize()).isEqualTo(7L);
                assertThat(statusListener.getRowCount()).isEqualTo(2L);
                assertThat(statusListener.getByteCount()).isEqualTo(7L);
                assertThat(statusListener.isCompleted()).isTrue();
                assertThat(statusListener.getThrowable()).isNull();
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
            try (var csv = buildSinglePage("012")) {
                assertThat(csv.pageCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(1);

                assertThat(csv.rowCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(1L);

                assertThat(csv.readPage(0))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012");
            }
        }

        // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
        @Test
        void twoLines() throws IOException {
            try (var csv = buildSinglePage("012,foo\n345,bar")) {
                assertThat(csv.pageCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(2);

                assertThat(csv.rowCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(2L);

                assertThat(csv.readPage(0))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012", TEST_STRING);

                assertThat(csv.readPage(1))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .singleElement(CSV_ROW)
                    .isOriginalLineNumber(2)
                    .isNotComment()
                    .fields().containsExactly("345", "bar");
            }
        }

    }

    @Nested
    class MultipleRowsPerPage {

        @Test
        void start0EndInfinite() throws IOException, ExecutionException, InterruptedException, TimeoutException {
            assertThat(readRows(0, 100))
                .flatMap(CsvRow::getFields)
                .containsExactly("1", "2", "3", "4", "5");
        }

        @Test
        void start1End2() throws IOException {
            final IndexedCsvReader csv = IndexedCsvReader.builder()
                .pageSize(2)
                .build(prepareTestFile("1\n2\n3\n4\n5"));

            try (csv) {
                assertThat(csv.pageCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(3);

                assertThat(csv.rowCount())
                    .succeedsWithin(TIMEOUT)
                    .isEqualTo(5L);

                assertThat(csv.readPage(0))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .satisfiesExactly(
                        item1 -> CsvRowAssert.assertThat(item1).fields().containsExactly("1"),
                        item2 -> CsvRowAssert.assertThat(item2).fields().containsExactly("2")
                    );

                assertThat(csv.readPage(1))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .satisfiesExactly(
                        item1 -> CsvRowAssert.assertThat(item1).fields().containsExactly("3"),
                        item2 -> CsvRowAssert.assertThat(item2).fields().containsExactly("4")
                    );

                assertThat(csv.readPage(2))
                    .succeedsWithin(TIMEOUT, CSV_PAGE)
                    .satisfiesExactly(
                        item1 -> CsvRowAssert.assertThat(item1).fields().containsExactly("5")
                    );
            }
        }

        private List<CsvRow> readRows(final int page, final int pageSize)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {

            return IndexedCsvReader.builder().pageSize(pageSize)
                .build(prepareTestFile("1\n2\n3\n4\n5"))
                .readPage(page)
                .get(1, TimeUnit.SECONDS);
        }

    }

}
