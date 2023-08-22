package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static testutil.NamedCsvRecordAssert.NAMED_CSV_RECORD;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
class NamedCsvReaderTest {

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder();

    @Test
    void empty() {
        final NamedCsvReader csv = parse("");

        assertThat(csv.getHeader())
            .isEmpty();

        assertThat(csv.iterator())
            .isExhausted()
            .satisfies(it -> assertThatThrownBy(it::next)
                .isInstanceOf(NoSuchElementException.class));
    }

    // toString()

    @Test
    void readerToString() {
        assertThat(crb.build("h1\nd1")).asString()
            .isEqualTo("NamedCsvReader[header=null, csvReader=CsvReader["
                + "commentStrategy=NONE, skipEmptyRecords=true, errorOnDifferentFieldCount=true]]");
    }

    @Test
    void duplicateHeader() {
        assertThatThrownBy(() -> parse("a,b,a").getHeader())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicate header field 'a' found");
    }

    @Test
    void onlyHeader() {
        final NamedCsvReader csv = parse("foo,bar\n");

        assertThat(csv.getHeader())
            .containsExactly("foo", "bar");

        assertThat(csv.iterator())
            .isExhausted()
            .satisfies(it -> assertThatThrownBy(it::next)
                .isInstanceOf(NoSuchElementException.class));
    }

    @Test
    void getFieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .field("foo").isEqualTo("bar");
    }

    @SuppressWarnings("JoinAssertThatStatements")
    @Test
    void getHeader() {
        assertThat(parse("foo\nbar").getHeader())
            .containsExactly("foo");

        final NamedCsvReader reader = parse("foo,bar\n1,2");
        assertThat(reader.getHeader())
            .containsExactly("foo", "bar");

        // second call (lazy init)
        assertThat(reader.getHeader())
            .containsExactly("foo", "bar");
    }

    @Test
    void getHeaderEmptyRecords() {
        final NamedCsvReader csv = parse("foo,bar");

        assertThat(csv.getHeader())
            .containsExactly("foo", "bar");

        assertThat(csv.iterator())
            .isExhausted()
            .satisfies(it -> assertThatThrownBy(it::next)
                .isInstanceOf(NoSuchElementException.class));
    }

    @Test
    void getHeaderAfterSkippedRecord() {
        final NamedCsvReader csv = parse("\nfoo,bar");

        assertThat(csv.getHeader())
            .containsExactly("foo", "bar");

        assertThat(csv.iterator())
            .isExhausted();
    }

    @Test
    void getHeaderWithoutNextRecordCall() {
        assertThat(parse("foo\n").getHeader())
            .containsExactly("foo");
    }

    @Test
    void findNonExistingFieldByName() {
        assertThatThrownBy(() -> parse("foo\nfaz").iterator().next().field("bar"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("No element with name 'bar' found. Valid names are: [foo]");
    }

    @Test
    void toStringWithHeader() {
        assertThat(parse("headerA,headerB,headerC\nfieldA,fieldB,fieldC\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("NamedCsvRecord[originalLineNumber=2, "
                + "fieldMap={headerA=fieldA, headerB=fieldB, headerC=fieldC}]");
    }

    @Test
    void fieldMap() {
        assertThat(parse("headerA,headerB,headerC\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("headerA", "fieldA"), entry("headerB", "fieldB"), entry("headerC", "fieldC"))
            .asString()
            .isEqualTo("{headerA=fieldA, headerB=fieldB, headerC=fieldC}");
    }

    // line numbering

    @Test
    void lineNumbering() {
        final Stream<NamedCsvRecord> stream = crb
            .build(
                "h1,h2\n"
                    + "a,line 2\n"
                    + "b,line 3\r"
                    + "c,line 4\r\n"
                    + "d,\"line 5\rwith\r\nand\n\"\n"
                    + "e,line 9"
            ).stream();

        assertThat(stream)
            .satisfiesExactly(
                item1 -> NamedCsvRecordAssert.assertThat(item1).isOriginalLineNumber(2)
                    .fields().containsOnly(entry("h1", "a"), entry("h2", "line 2")),
                item2 -> NamedCsvRecordAssert.assertThat(item2).isOriginalLineNumber(3)
                    .fields().containsOnly(entry("h1", "b"), entry("h2", "line 3")),
                item3 -> NamedCsvRecordAssert.assertThat(item3).isOriginalLineNumber(4)
                    .fields().containsOnly(entry("h1", "c"), entry("h2", "line 4")),
                item4 -> NamedCsvRecordAssert.assertThat(item4).isOriginalLineNumber(5)
                    .fields().containsOnly(entry("h1", "d"), entry("h2", "line 5\rwith\r\nand\n")),
                item5 -> NamedCsvRecordAssert.assertThat(item5).isOriginalLineNumber(9)
                    .fields().containsOnly(entry("h1", "e"), entry("h2", "line 9"))
            );
    }

    // API

    @Test
    void closeApi() throws IOException {
        final Consumer<NamedCsvRecord> consumer = csvRecord -> { };

        final Supplier<CloseStatusReader> supp =
            () -> new CloseStatusReader(new StringReader("h1,h2\nfoo,bar"));

        CloseStatusReader csr = supp.get();

        try (NamedCsvReader reader = crb.build(csr)) {
            reader.stream().forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (CloseableIterator<NamedCsvRecord> it = crb.build(csr).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (Stream<NamedCsvRecord> stream = crb.build(csr).stream()) {
            stream.forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();
    }

    @Test
    void noComments() {
        assertThat(readAll("# comment 1\nfieldA").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields().containsExactly(entry("# comment 1", "fieldA"));
    }

    @Test
    void spliterator() {
        final Spliterator<NamedCsvRecord> spliterator =
            crb.build("a,b,c\n1,2,3\n4,5,6").spliterator();

        assertThat(spliterator.trySplit()).isNull();
        assertThat(spliterator.estimateSize()).isEqualTo(Long.MAX_VALUE);

        final AtomicInteger records = new AtomicInteger();
        final AtomicInteger records2 = new AtomicInteger();
        while (spliterator.tryAdvance(csvRecord -> records.incrementAndGet())) {
            records2.incrementAndGet();
        }

        assertThat(records).hasValue(2);
        assertThat(records2).hasValue(2);
    }

    // Coverage

    @Test
    void closeException() {
        final NamedCsvReader csvReader = crb
            .build(new UncloseableReader(new StringReader("foo")));

        assertThatThrownBy(() -> csvReader.stream().close())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot close");
    }

    // test helpers

    private NamedCsvReader parse(final String data) {
        return crb.build(data);
    }

    private List<NamedCsvRecord> readAll(final String data) {
        return parse(data).stream().collect(Collectors.toList());
    }

}
