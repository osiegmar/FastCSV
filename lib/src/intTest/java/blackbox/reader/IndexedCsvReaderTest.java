package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static testutil.CsvRecordAssert.CSV_RECORD;
import static testutil.NamedCsvRecordAssert.NAMED_CSV_RECORD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CollectingStatusListener;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvIndex;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.IndexedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;
import testutil.CsvRecordAssert;

@ExtendWith(SoftAssertionsExtension.class)
class IndexedCsvReaderTest {

    private static final String TEST_STRING = "foo";
    private static final String NON_EXISTENT_FILENAME = "/tmp/non-existent";

    @InjectSoftAssertions
    private SoftAssertions softly;

    @TempDir
    private Path tmpDir;

    @Test
    void outOfBounds() throws IOException {
        try (var csv = buildSinglePage("")) {
            final CsvIndex index = csv.getIndex();

            softly.assertThat(index.getPageCount())
                .isZero();

            softly.assertThat(index.getRecordCount())
                .isZero();

            softly.assertThatThrownBy(() -> csv.readPage(0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index 0 out of bounds for length 0");
        }
    }

    @Test
    void readerToString() throws IOException {
        final Path file = prepareTestFile(TEST_STRING);

        assertThat(singlePageBuilder().ofCsvRecord(file)).asString()
            .isEqualTo("IndexedCsvReader[file=%s, charset=UTF-8, fieldSeparator=,, "
                    + "quoteCharacter=\", commentStrategy=NONE, commentCharacter=#, pageSize=1, "
                    + "index=CsvIndex[bomHeaderLength=0, fileSize=3, fieldSeparator=44, quoteCharacter=34, "
                    + "commentStrategy=NONE, commentCharacter=35, recordCount=1, pageCount=1]]",
                file);
    }

    // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
    @Test
    void unicode() throws IOException {
        try (var csv = buildSinglePage("abc\nüöä\nabc")) {
            final CsvIndex index = csv.getIndex();

            softly.assertThat(index.getPageCount())
                .isEqualTo(3);

            softly.assertThat(index.getRecordCount())
                .isEqualTo(3L);

            assertThat(csv.readPage(0))
                .singleElement(CSV_RECORD)
                .fields().singleElement().isEqualTo("abc");

            assertThat(csv.readPage(1))
                .singleElement(CSV_RECORD)
                .fields().singleElement().isEqualTo("üöä");

            assertThat(csv.readPage(2))
                .singleElement(CSV_RECORD)
                .fields().singleElement().isEqualTo("abc");
        }
    }

    @Test
    void explicitCharset() throws IOException {
        try (var csv = singlePageBuilder().ofCsvRecord(prepareTestFile("abc\nüöä\nabc"), UTF_8)) {
            final CsvIndex index = csv.getIndex();

            assertThat(index.getPageCount())
                .isEqualTo(3);
        }
    }

    @Test
    void namedCsv() throws IOException {
        final var icrb = IndexedCsvReader.builder().pageSize(2);
        final var cbh = new NamedCsvRecordHandler();

        try (var csv = icrb.build(cbh, prepareTestFile("h1\nv1\nv2"))) {
            final CsvIndex index = csv.getIndex();

            assertThat(index.getPageCount())
                .isEqualTo(2);

            assertThat(index.getRecordCount())
                .isEqualTo(3L);

            assertThat(csv.readPage(0))
                .singleElement(NAMED_CSV_RECORD)
                .fields().containsExactly(entry("h1", "v1"));

            assertThat(csv.readPage(1))
                .singleElement(NAMED_CSV_RECORD)
                .fields().containsExactly(entry("h1", "v2"));
        }
    }

    private IndexedCsvReader<CsvRecord> buildSinglePage(final String data) throws IOException {
        return singlePageBuilder().ofCsvRecord(prepareTestFile(data));
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

            softly.assertThatThrownBy(() -> singlePageBuilder().fieldSeparator('"').ofCsvRecord(emptyFile))
                .as("fieldSeparator")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, "\"", "\"", "#");

            softly.assertThatThrownBy(() -> singlePageBuilder().quoteCharacter('#').ofCsvRecord(emptyFile))
                .as("quoteCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", "#", "#");

            softly.assertThatThrownBy(() -> singlePageBuilder().commentCharacter(',').ofCsvRecord(emptyFile))
                .as("commentCharacter")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage, ",", "\"", ",");
        }

        @Test
        void nullFile() {
            assertThatThrownBy(() -> singlePageBuilder().ofCsvRecord(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("file must not be null");
        }

        @Test
        void nonExistingFile() {
            assertThatThrownBy(() -> singlePageBuilder()
                .ofCsvRecord(Path.of(NON_EXISTENT_FILENAME)))
                .isInstanceOf(NoSuchFileException.class)
                .hasMessage(NON_EXISTENT_FILENAME);
        }

        @Test
        void nonExistingFileWithListener() {
            final var statusListener = new CollectingStatusListener();

            assertThatThrownBy(() -> singlePageBuilder()
                .statusListener(statusListener)
                .ofCsvRecord(Path.of(NON_EXISTENT_FILENAME)));

            assertThat(statusListener.getThrowable())
                .isInstanceOf(NoSuchFileException.class)
                .hasMessage(NON_EXISTENT_FILENAME);
        }

        @Test
        void illegalPage() {
            assertThatThrownBy(() -> buildSinglePage(TEST_STRING).readPage(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be >= 0");
        }

        @Test
        void pageOutOfBounds() {
            assertThatThrownBy(() -> buildSinglePage(TEST_STRING).readPage(10))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index 10 out of bounds for length 1");
        }

        @ParameterizedTest
        @NullSource
        void nullCharset(final Charset charset) {
            assertThatThrownBy(() -> singlePageBuilder().ofCsvRecord(Paths.get("/tmp"), charset))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("charset must not be null");
        }

        @Test
        void zeroPageSize() {
            assertThatThrownBy(() -> singlePageBuilder().pageSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageSize must be >= 1");
        }

        @Test
        void invalidIndex() throws IOException {
            final Path file = prepareTestFile(TEST_STRING);

            final IndexedCsvReader.IndexedCsvReaderBuilder builder =
                IndexedCsvReader.builder();

            final CsvIndex index = builder
                .ofCsvRecord(file)
                .getIndex();

            assertThatThrownBy(() -> builder.index(index).fieldSeparator(';').ofCsvRecord(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Index does not match! Expected: bomHeaderLength=0, fileSize=3, fieldSeparator=59, "
                    + "quoteCharacter=34, commentStrategy=NONE, commentCharacter=35; Actual: bomHeaderLength=0, "
                    + "fileSize=3, fieldSeparator=44, quoteCharacter=34, commentStrategy=NONE, commentCharacter=35");
        }

    }

    @Nested
    class CsvComment {

        @Test
        void readComment() throws IOException {
            final Path file = prepareTestFile("foo\n#a,b,c\nbaz");

            final var csv = singlePageBuilder()
                .commentStrategy(CommentStrategy.READ)
                .ofCsvRecord(file);

            try (csv) {
                assertThat(csv.readPage(0))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("foo");

                assertThat(csv.readPage(1))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(2)
                    .isComment()
                    .fields().containsExactly("a,b,c");

                assertThat(csv.readPage(2))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(3)
                    .isNotComment()
                    .fields().containsExactly("baz");
            }
        }

        @Test
        void noneComment() throws IOException {
            final Path file = prepareTestFile("foo\n#a,b,c\nbaz");

            final IndexedCsvReader<CsvRecord> csv = singlePageBuilder()
                .commentStrategy(CommentStrategy.NONE)
                .ofCsvRecord(file);

            try (csv) {
                assertThat(csv.readPage(0))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("foo");

                assertThat(csv.readPage(1))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(2)
                    .isNotComment()
                    .fields().containsExactly("#a", "b", "c");

                assertThat(csv.readPage(2))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(3)
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
    class CsvStatus {

        @Test
        void finalStatus() throws IOException {
            final var statusListener = new CollectingStatusListener();

            final IndexedCsvReader<CsvRecord> csv = singlePageBuilder()
                .statusListener(statusListener)
                .ofCsvRecord(prepareTestFile("foo\nbar"));

            try (csv) {
                assertThat(statusListener.getFileSize()).isEqualTo(7L);
                assertThat(statusListener.getRecordCount()).isEqualTo(2L);
                assertThat(statusListener.getByteCount()).isEqualTo(7L);
                assertThat(statusListener.isCompleted()).isTrue();
                assertThat(statusListener.getThrowable()).isNull();
                assertThat(statusListener).asString()
                    .isEqualTo("Read %,d records and %,d of %,d bytes (%.2f %%)", 2, 7, 7, 100.0);
            }
        }

        @Test
        void emptyFileStatus() throws IOException {
            final var statusListener = new CollectingStatusListener();

            final IndexedCsvReader<CsvRecord> csv = singlePageBuilder()
                .statusListener(statusListener)
                .ofCsvRecord(prepareTestFile(""));

            try (csv) {
                assertThat(statusListener).asString()
                    .isEqualTo("Read 0 records and 0 of 0 bytes (NaN %)");
            }
        }

    }

    @Nested
    class SingleRead {

        // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
        @Test
        void oneLine() throws IOException {
            try (var csv = buildSinglePage("012")) {
                final CsvIndex index = csv.getIndex();

                assertThat(index.getPageCount()).isOne();

                assertThat(index.getRecordCount()).isOne();

                assertThat(csv.readPage(0))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012");
            }
        }

        // Softly does not work (IllegalAccessException: module org.assertj.core does not read module common)
        @Test
        void twoLines() throws IOException {
            try (var csv = buildSinglePage("012,foo\n345,bar")) {
                final CsvIndex index = csv.getIndex();

                assertThat(index.getPageCount())
                    .isEqualTo(2);

                assertThat(index.getRecordCount())
                    .isEqualTo(2L);

                assertThat(csv.readPage(0))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(1)
                    .isNotComment()
                    .fields().containsExactly("012", TEST_STRING);

                assertThat(csv.readPage(1))
                    .singleElement(CSV_RECORD)
                    .isStartingLineNumber(2)
                    .isNotComment()
                    .fields().containsExactly("345", "bar");
            }
        }

    }

    @Nested
    class MultipleRecordsPerPage {

        @Test
        void start0EndInfinite() throws IOException {
            assertThat(readRecords(0, 100))
                .flatMap(CsvRecord::getFields)
                .containsExactly("1", "2", "3", "4", "5");
        }

        @Test
        void start1End2() throws IOException {
            final IndexedCsvReader<CsvRecord> csv = IndexedCsvReader.builder()
                .pageSize(2)
                .ofCsvRecord(prepareTestFile("1\n2a,2b\n\"3\nfoo\"\n4\n5"));

            try (csv) {
                final CsvIndex index = csv.getIndex();

                assertThat(index.getPageCount())
                    .isEqualTo(3);

                assertThat(index.getRecordCount())
                    .isEqualTo(5L);

                assertThat(csv.readPage(0))
                    .satisfiesExactly(
                        item1 -> CsvRecordAssert.assertThat(item1)
                            .isStartingLineNumber(1)
                            .fields().containsExactly("1"),
                        item2 -> CsvRecordAssert.assertThat(item2)
                            .isStartingLineNumber(2)
                            .fields().containsExactly("2a", "2b")
                    );

                assertThat(csv.readPage(1))
                    .satisfiesExactly(
                        item1 -> CsvRecordAssert.assertThat(item1)
                            .isStartingLineNumber(3)
                            .fields().containsExactly("3\nfoo"),
                        item2 -> CsvRecordAssert.assertThat(item2)
                            .isStartingLineNumber(5)
                            .fields().containsExactly("4")
                    );

                assertThat(csv.readPage(2))
                    .satisfiesExactly(
                        item1 -> CsvRecordAssert.assertThat(item1)
                            .isStartingLineNumber(6)
                            .fields().containsExactly("5")
                    );
            }
        }

        private List<CsvRecord> readRecords(final int page, final int pageSize) throws IOException {
            return IndexedCsvReader.builder().pageSize(pageSize)
                .ofCsvRecord(prepareTestFile("1\n2\n3\n4\n5"))
                .readPage(page);
        }

    }

    @Nested
    class IndexSerialization {

        @Test
        void serializeIndex() throws IOException, ClassNotFoundException {
            final Path file = prepareTestFile(TEST_STRING);

            final CsvIndex expectedIndex;
            try (IndexedCsvReader<CsvRecord> expectedCsv = singlePageBuilder()
                .ofCsvRecord(file)) {

                assertThat(expectedCsv.readPage(0))
                    .singleElement()
                    .extracting(e -> e.getField(0))
                    .isEqualTo(TEST_STRING);

                expectedIndex = expectedCsv.getIndex();
            }

            final byte[] serialize = serialize(expectedIndex);
            final CsvIndex actualIndex = deserialize(serialize);

            assertThat(actualIndex)
                .hasSameHashCodeAs(expectedIndex)
                .isEqualTo(expectedIndex);

            try (IndexedCsvReader<CsvRecord> actualCsv = singlePageBuilder()
                .index(actualIndex)
                .ofCsvRecord(file)) {

                assertThat(actualCsv.readPage(0))
                    .singleElement()
                    .extracting(e -> e.getField(0))
                    .isEqualTo(TEST_STRING);
            }
        }

        private byte[] serialize(final CsvIndex index) throws IOException {
            try (var baos = new ByteArrayOutputStream();
                 var oos = new ObjectOutputStream(baos)) {
                oos.writeObject(index);
                return baos.toByteArray();
            }
        }

        private CsvIndex deserialize(final byte[] serialize) throws IOException, ClassNotFoundException {
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serialize))) {
                return (CsvIndex) ois.readObject();
            }
        }

    }

}
