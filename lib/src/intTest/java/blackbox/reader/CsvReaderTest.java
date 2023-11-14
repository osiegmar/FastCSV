package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.MalformedCsvException;
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
        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(c).build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("fieldSeparator must not be a newline char");

        assertThatThrownBy(() -> CsvReader.builder().quoteCharacter(c).build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("quoteCharacter must not be a newline char");

        assertThatThrownBy(() -> CsvReader.builder().commentCharacter(c).build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("commentCharacter must not be a newline char");
    }

    @Test
    void configReader() {
        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(',').quoteCharacter(',').build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=#)");

        assertThatThrownBy(() -> CsvReader.builder().fieldSeparator(',').commentCharacter(',').build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=\", commentCharacter=,)");

        assertThatThrownBy(() -> CsvReader.builder().quoteCharacter(',').commentCharacter(',').build("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Control characters must differ (fieldSeparator=,, quoteCharacter=,, commentCharacter=,)");
    }

    @Test
    void empty() {
        assertThat(crb.build("").iterator())
            .isExhausted()
            .satisfies(it -> assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class));
    }

    @Test
    void immutableResponse() {
        final List<String> fields = crb.build("foo").iterator().next().fields();
        assertThatThrownBy(() -> fields.add("bar"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // toString()

    @Test
    void readerToString() {
        assertThat(crb.build(""))
            .asString()
            .isEqualTo("CsvReader[commentStrategy=NONE, skipEmptyLines=true, "
                + "ignoreDifferentFieldCount=true]");
    }

    // skipped record

    @Test
    void singleRecordNoSkipEmpty() {
        crb.skipEmptyLines(false);
        assertThat(crb.build("").iterator()).isExhausted();
    }

    @Test
    void multipleRecordsNoSkipEmpty() {
        crb.skipEmptyLines(false);

        assertThat(crb.build("\n\na").iterator()).toIterable()
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isOriginalLineNumber(1).fields().containsExactly(""),
                item2 -> CsvRecordAssert.assertThat(item2).isOriginalLineNumber(2).fields().containsExactly(""),
                item3 -> CsvRecordAssert.assertThat(item3).isOriginalLineNumber(3).fields().containsExactly("a"));
    }

    @Test
    void skippedRecords() {
        assertThat(crb.build("\n\nfoo\n\nbar\n\n").stream())
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isOriginalLineNumber(3).fields().containsExactly("foo"),
                item2 -> CsvRecordAssert.assertThat(item2).isOriginalLineNumber(5).fields().containsExactly("bar")
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
        assertThat(crb.ignoreDifferentFieldCount(true).build(s).stream())
            .isNotEmpty();
    }

    @Test
    void differentFieldCountFail() {
        crb.ignoreDifferentFieldCount(false);

        assertThatThrownBy(() -> readAll("foo\nbar,\"baz\nbax\""))
            .isInstanceOf(MalformedCsvException.class)
            .hasMessage("Record 2 has 2 fields, but first record had 1 fields");
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    void getNonExistingFieldByIndex() {
        assertThat(crb.build("foo").stream())
            .singleElement()
            .satisfies(item -> assertThatThrownBy(() -> spotbugs(item.field(1)))
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
            .build(
                "line 1\n"
                    + "line 2\r"
                    + "line 3\r\n"
                    + "\"line 4\rwith\r\nand\n\"\n"
                    + "#line 8\n"
                    + "line 9"
            ).stream();

        assertThat(stream)
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isOriginalLineNumber(1)
                    .fields().containsExactly("line 1"),
                item2 -> CsvRecordAssert.assertThat(item2).isOriginalLineNumber(2)
                    .fields().containsExactly("line 2"),
                item3 -> CsvRecordAssert.assertThat(item3).isOriginalLineNumber(3)
                    .fields().containsExactly("line 3"),
                item4 -> CsvRecordAssert.assertThat(item4).isOriginalLineNumber(4)
                    .fields().containsExactly("line 4\rwith\r\nand\n"),
                item5 -> CsvRecordAssert.assertThat(item5).isOriginalLineNumber(9)
                    .fields().containsExactly("line 9")
            );
    }

    // comment

    @Test
    void comment() {
        final Stream<CsvRecord> stream = crb
            .commentStrategy(CommentStrategy.READ)
            .build("#comment \"1\"\na,#b,c").stream();

        assertThat(stream)
            .satisfiesExactly(
                item1 -> CsvRecordAssert.assertThat(item1).isOriginalLineNumber(1)
                    .fields().containsExactly("comment \"1\""),
                item2 -> CsvRecordAssert.assertThat(item2).isOriginalLineNumber(2)
                    .fields().containsExactly("a", "#b", "c")
            );
    }

    // to string

    @Test
    void toStringWithoutHeader() {
        assertThat(crb.build("fieldA,fieldB\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("CsvRecord[originalLineNumber=1, fields=[fieldA, fieldB], comment=false]");
    }

    // refill buffer while parsing an unquoted field containing a quote character

    @Test
    void refillBufferInDataWithQuote() {
        final char[] extra = ",a\"b\"c,d,".toCharArray();

        final char[] buf = new char[8192 + extra.length];
        Arrays.fill(buf, 'X');
        System.arraycopy(extra, 0, buf, 8190, extra.length);

        assertThat(crb.build(new CharArrayReader(buf)).stream())
            .singleElement(CSV_RECORD)
            .fields().hasSize(4)
            .endsWith("a\"b\"c", "d", "XX");
    }

    // buffer exceed

    @Test
    void bufferExceed() {
        final char[] buf = new char[8 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        buf[buf.length - 1] = ',';

        crb.build(new CharArrayReader(buf)).iterator().next();

        buf[buf.length - 1] = (byte) 'X';

        assertThatThrownBy(() -> crb.build(new CharArrayReader(buf)).iterator().next())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("IOException when reading first record")
            .rootCause().hasMessage("Maximum buffer size 8388608 is not enough to read data of a single field. "
                + "Typically, this happens if quotation started but did not end within this buffer's "
                + "maximum boundary.");
    }

    @Test
    void bufferExceedSubsequentRecord() {
        final char[] buf = new char[8 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        final String s = "a,b,c\n\"";
        System.arraycopy(s.toCharArray(), 0, buf, 0, s.length());

        final CloseableIterator<CsvRecord> iterator = crb.build(new CharArrayReader(buf)).iterator();
        iterator.next();

        assertThatThrownBy(iterator::next)
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("IOException when reading record that started in line 2")
            .rootCause().hasMessage("Maximum buffer size 8388608 is not enough to read data of a single field. "
                + "Typically, this happens if quotation started but did not end within this buffer's "
                + "maximum boundary.");
    }

    // API

    @Test
    void closeApi() throws IOException {
        final Consumer<CsvRecord> consumer = csvRecord -> {
        };

        final Supplier<CloseStatusReader> supp =
            () -> new CloseStatusReader(new StringReader("foo,bar"));

        CloseStatusReader csr = supp.get();
        try (CsvReader reader = crb.build(csr)) {
            reader.stream().forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (CloseableIterator<CsvRecord> it = crb.build(csr).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (Stream<CsvRecord> stream = crb.build(csr).stream()) {
            stream.forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();
    }

    @Test
    void closeStringNoException() {
        assertThatCode(() -> crb.build("foo").close())
            .doesNotThrowAnyException();
    }

    @Test
    void spliterator() {
        final Spliterator<CsvRecord> spliterator =
            crb.build("a,b,c\n1,2,3").spliterator();

        assertThat(spliterator.trySplit()).isNull();
        assertThat(spliterator.estimateSize()).isEqualTo(Long.MAX_VALUE);
        assertThat(spliterator.characteristics())
            .isEqualTo(Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL
                | Spliterator.IMMUTABLE);

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
        assertThat(crb.build("foo\nfoo").stream().parallel().distinct().count())
            .isEqualTo(2);
    }

    // Coverage

    @Test
    void closeException() {
        assertThatThrownBy(() -> crb.build(new UncloseableReader(new StringReader("foo"))).stream().close())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot close");
    }

    @Test
    void unreadable() {
        assertThatThrownBy(() -> crb.build(new UnreadableReader()).iterator().next())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("IOException when reading first record");
    }

    // test helpers

    private List<CsvRecord> readAll(final String data) {
        return crb.build(data).stream().collect(Collectors.toList());
    }

}
