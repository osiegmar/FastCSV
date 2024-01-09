package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static testutil.CsvRecordAssert.CSV_RECORD;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import testutil.CsvRecordAssert;

@SuppressWarnings({
    "checkstyle:ClassFanOutComplexity",
    "PMD.AvoidDuplicateLiterals",
    "PMD.CloseResource"
})
class CsvReaderTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @ParameterizedTest
    @ValueSource(chars = {'\r', '\n'})
    void configBuilder(final char c) {
        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(c).ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("fieldSeparator must not be a newline char");

        assertThatThrownBy(() -> CsvReader.builder().quoteCharacter(c).ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("quoteCharacter must not be a newline char");

        assertThatThrownBy(() -> CsvReader.builder().commentCharacter(c).ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("commentCharacter must not be a newline char");
    }

    @Test
    void configReader() {
        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(',').quoteCharacter(',').ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=#)");

        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(',').commentCharacter(',').ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=\", commentCharacter=,)");

        assertThatThrownBy(() -> CsvReader.builder().quoteCharacter(',').commentCharacter(',').ofCsvRecord("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=,)");
    }

    @Test
    void empty() {
        assertThat(crb.ofCsvRecord("").iterator())
            .isExhausted()
            .satisfies(it -> assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class));
    }

    @Test
    void immutableResponse() {
        final List<String> fields = crb.ofCsvRecord("foo").iterator().next().getFields();
        assertThatThrownBy(() -> fields.add("bar"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // toString()

    @Test
    void readerToString() {
        assertThat(crb.ofCsvRecord(""))
            .asString()
            .isEqualTo("CsvReader[commentStrategy=NONE, skipEmptyLines=true, "
                + "ignoreDifferentFieldCount=true]");
    }

    // skipped record

    @Test
    void singleRecordNoSkipEmpty() {
        crb.skipEmptyLines(false);
        assertThat(crb.ofCsvRecord("").iterator()).isExhausted();
    }

    @Test
    void multipleRecordsNoSkipEmpty() {
        crb.skipEmptyLines(false);

        assertThat(crb.ofCsvRecord("\n\na").iterator()).toIterable()
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isStartingLineNumber(1).fields().containsExactly(""),
                item2 -> CsvRecordAssert.assertThat(item2).isStartingLineNumber(2).fields().containsExactly(""),
                item3 -> CsvRecordAssert.assertThat(item3).isStartingLineNumber(3).fields().containsExactly("a"));
    }

    @Test
    void skippedRecords() {
        assertThat(crb.ofCsvRecord("\n\nfoo\n\nbar\n\n").stream())
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isStartingLineNumber(3).fields().containsExactly("foo"),
                item2 -> CsvRecordAssert.assertThat(item2).isStartingLineNumber(5).fields().containsExactly("bar")
            );
    }

    // different field count

    @ParameterizedTest
    @ValueSource(strings = {
        "foo\nbar",
        "foo\nbar\n",
        "foo,bar\nfaz,baz",
        "foo,bar\nfaz,baz\n",
        "foo,bar\n,baz",
        ",bar\nfaz,baz"
    })
    void differentFieldCountSuccess(final String s) {
        assertThatNoException().isThrownBy(() -> readAll(s));
    }

    @Test
    void differentFieldCountSuccess2() {
        crb.ignoreDifferentFieldCount(false);
        assertThatNoException().isThrownBy(() -> readAll("foo\nbar"));
    }

    @Test
    void differentFieldCountFail() {
        crb.ignoreDifferentFieldCount(false);

        assertThatThrownBy(() -> readAll("foo\nbar,\"baz\nbax\""))
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading record that started in line 2")
            .hasRootCauseInstanceOf(CsvParseException.class)
            .hasRootCauseMessage("Record 2 has 2 fields, but first record had 1 fields");
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    void getNonExistingFieldByIndex() {
        assertThat(crb.ofCsvRecord("foo").stream())
            .singleElement()
            .satisfies(item -> assertThatThrownBy(() -> spotbugs(item.getField(1)))
                .isInstanceOf(IndexOutOfBoundsException.class));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void spotbugs(final String foo) {
        // Prevent RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    }

    // line numbering

    @Test
    void lineNumbering() {
        final Stream<CsvRecord> stream = crb
            .commentStrategy(CommentStrategy.SKIP)
            .ofCsvRecord("""
                line 1
                line 2\rline 3\r
                "line 4\rwith\r
                and
                "
                #line 8
                line 9"""
            ).stream();

        assertThat(stream)
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isStartingLineNumber(1)
                    .fields().containsExactly("line 1"),
                item2 -> CsvRecordAssert.assertThat(item2).isStartingLineNumber(2)
                    .fields().containsExactly("line 2"),
                item3 -> CsvRecordAssert.assertThat(item3).isStartingLineNumber(3)
                    .fields().containsExactly("line 3"),
                item4 -> CsvRecordAssert.assertThat(item4).isStartingLineNumber(4)
                    .fields().containsExactly("line 4\rwith\r\nand\n"),
                item5 -> CsvRecordAssert.assertThat(item5).isStartingLineNumber(9)
                    .fields().containsExactly("line 9")
            );
    }

    // comment

    @Test
    void comment() {
        final Stream<CsvRecord> stream = crb
            .commentStrategy(CommentStrategy.READ)
            .ofCsvRecord("#comment \"1\"\na,#b,c").stream();

        assertThat(stream)
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isStartingLineNumber(1)
                    .fields().containsExactly("comment \"1\""),
                item2 -> CsvRecordAssert.assertThat(item2).isStartingLineNumber(2)
                    .fields().containsExactly("a", "#b", "c")
            );
    }

    // to string

    @Test
    void toStringWithoutHeader() {
        assertThat(crb.ofCsvRecord("fieldA,fieldB\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("CsvRecord[startingLineNumber=1, fields=[fieldA, fieldB], comment=false]");
    }

    // refill buffer while parsing an unquoted field containing a quote character

    @Test
    void refillBufferInDataWithQuote() {
        final char[] extra = ",a\"b\"c,d,".toCharArray();

        final char[] buf = new char[8192 + extra.length];
        Arrays.fill(buf, 'X');
        System.arraycopy(extra, 0, buf, 8190, extra.length);

        assertThat(crb.ofCsvRecord(new CharArrayReader(buf)).stream())
            .singleElement(CSV_RECORD)
            .fields().hasSize(4)
            .endsWith("a\"b\"c", "d", "XX");
    }

    // field count exceed

    @Test
    void fieldCountExceed() {
        final char[] buf = new char[16 * 1024];
        Arrays.fill(buf, ',');

        assertThatThrownBy(() -> crb.ofCsvRecord(new CharArrayReader(buf)).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading first record")
            .hasRootCauseInstanceOf(CsvParseException.class)
            .hasRootCauseMessage("Maximum number of fields exceeded: %d", buf.length);
    }

    // buffer exceed

    @Test
    void bufferExceed() {
        final char[] buf = new char[16 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        buf[buf.length - 1] = ',';

        crb.ofCsvRecord(new CharArrayReader(buf)).iterator().next();

        buf[buf.length - 1] = (byte) 'X';

        assertThatThrownBy(() -> crb.ofCsvRecord(new CharArrayReader(buf)).iterator().next())
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Exception when reading first record")
            .rootCause()
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("is insufficient to read the data of a single field");
    }

    @Test
    void bufferExceedSubsequentRecord() {
        final char[] buf = new char[16 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        final String s = "a,b,c\n\"";
        System.arraycopy(s.toCharArray(), 0, buf, 0, s.length());

        final CloseableIterator<CsvRecord> iterator = crb.ofCsvRecord(new CharArrayReader(buf)).iterator();
        iterator.next();

        assertThatThrownBy(iterator::next)
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading record that started in line 2")
            .rootCause()
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("is insufficient to read the data of a single field");
    }

    // record size exceed

    @Test
    void recordSizeExceed() {
        final int records = 10;
        final int fieldSize = 8 * 1024 * 1024;
        final char[] buf = new char[records * fieldSize];
        Arrays.fill(buf, 'X');
        for (int i = 1; i < records; i++) {
            buf[i * fieldSize] = ',';
        }

        assertThatThrownBy(() -> crb.ofCsvRecord(new CharArrayReader(buf)).stream().count())
            .isInstanceOf(CsvParseException.class)
            .hasMessage("Exception when reading first record")
            .hasRootCauseInstanceOf(CsvParseException.class)
            .hasRootCauseMessage("Record starting at line 1 has surpassed the maximum limit of 67108864 characters");
    }

    // API

    @Test
    void closeApi() throws IOException {
        final Consumer<CsvRecord> consumer = csvRecord -> {
        };

        final Supplier<CloseStatusReader> supp =
            () -> new CloseStatusReader(new StringReader("foo,bar"));

        CloseStatusReader csr = supp.get();
        try (CsvReader<CsvRecord> reader = crb.ofCsvRecord(csr)) {
            reader.stream().forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (CloseableIterator<CsvRecord> it = crb.ofCsvRecord(csr).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (Stream<CsvRecord> stream = crb.ofCsvRecord(csr).stream()) {
            stream.forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();
    }

    @Test
    void closeStringNoException() {
        assertThatCode(() -> crb.ofCsvRecord("foo").close())
            .doesNotThrowAnyException();
    }

    @Test
    void spliterator() {
        final Spliterator<CsvRecord> spliterator =
            crb.ofCsvRecord("a,b,c\n1,2,3").spliterator();

        assertThat(spliterator.trySplit()).isNull();
        assertThat(spliterator.estimateSize()).isEqualTo(Long.MAX_VALUE);
        assertThat(spliterator.characteristics())
            .isEqualTo(Spliterator.ORDERED | Spliterator.NONNULL);

        final var csvRecords = new AtomicInteger();
        final var csvRecords2 = new AtomicInteger();
        while (spliterator.tryAdvance(csvRecord -> csvRecords.incrementAndGet())) {
            csvRecords2.incrementAndGet();
        }

        assertThat(csvRecords).hasValue(2);
        assertThat(csvRecords2).hasValue(2);
    }

    @Test
    void parallelDistinct() {
        assertThat(crb.ofCsvRecord("foo\nfoo").stream().parallel().distinct().count())
            .isEqualTo(2);
    }

    // Coverage

    @Test
    void closeException() {
        assertThatThrownBy(() -> crb.ofCsvRecord(new UncloseableReader(new StringReader("foo"))).stream().close())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot close");
    }

    @Test
    void unreadable() {
        assertThatThrownBy(() -> crb.ofCsvRecord(new UnreadableReader()).iterator().next())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("Exception when reading first record")
            .hasRootCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("Cannot read");
    }

    @Test
    void fieldCount() {
        assertThat(crb.ofCsvRecord("foo,bar").iterator().next().getFieldCount()).isEqualTo(2);
    }

    // test helpers

    private List<CsvRecord> readAll(final String data) {
        return crb.ofCsvRecord(data).stream().collect(Collectors.toList());
    }

}
