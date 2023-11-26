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
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import testutil.NamedCsvRecordAssert;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
class NamedCsvReaderTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

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
        assertThat(NamedCsvReader.from(crb.build("h1\nd1"))).asString()
            .isEqualTo("NamedCsvReader[header=null, csvReader=CsvReader["
                + "commentStrategy=NONE, skipEmptyLines=true, ignoreDifferentFieldCount=true]]");
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

    @Test
    void findFieldByName() {
        assertThat(parse("foo\nbar").stream())
            .singleElement(NAMED_CSV_RECORD)
            .findField("foo").hasValue("bar");
    }

    @Test
    void findFieldsByName() {
        assertThat(parse("foo,xoo,foo\nbar,moo,baz").stream())
            .singleElement(NAMED_CSV_RECORD)
            .findFields("foo").containsExactly("bar", "baz");
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
    void getHeaderEmptyLines() {
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
    void getNonExistingFieldByName() {
        assertThatThrownBy(() -> parse("foo\nfaz").iterator().next().getField("bar"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Header does not contain a field 'bar'. Valid names are: [foo]");
    }

    @Test
    void findNonExistingFieldByName() {
        assertThat(parse("foo\nfaz").iterator().next().findField("bar"))
            .isEmpty();
    }

    @Test
    void findNonExistingFieldByName2() {
        assertThat(parse("foo,bar\nfaz").iterator().next().findField("bar"))
            .isEmpty();
    }

    @Test
    void toStringWithHeader() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement()
            .asString()
            .isEqualTo("NamedCsvRecord[originalLineNumber=2, "
                + "fields=[fieldA, fieldB, fieldC], "
                + "comment=false, "
                + "header=[headerA, headerB, headerA]]");
    }

    @Test
    void fieldMap() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("headerA", "fieldA"), entry("headerB", "fieldB"));
    }

    @Test
    void allFieldsMap() {
        assertThat(parse("headerA,headerB,headerA\nfieldA,fieldB,fieldC\n").stream())
            .singleElement(NAMED_CSV_RECORD)
            .allFields()
            .containsOnly(entry("headerA", List.of("fieldA", "fieldC")), entry("headerB", List.of("fieldB")));
    }

    @Test
    void customHeader() {
        final List<String> myHeader = List.of("h1", "h2");

        final NamedCsvReader csvReader = NamedCsvReader.from(CsvReader.builder().build("foo,bar"), myHeader);
        assertThat(csvReader.stream())
            .singleElement(NAMED_CSV_RECORD)
            .fields()
            .containsExactly(entry("h1", "foo"), entry("h2", "bar"));
    }

    // line numbering

    @Test
    void lineNumbering() {
        final Stream<NamedCsvRecord> stream = NamedCsvReader.from(crb
            .build(
                "h1,h2\n"
                    + "a,line 2\n"
                    + "b,line 3\r"
                    + "c,line 4\r\n"
                    + "d,\"line 5\rwith\r\nand\n\"\n"
                    + "e,line 9"
            )).stream();

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

        try (NamedCsvReader reader = NamedCsvReader.from(crb.build(csr))) {
            reader.stream().forEach(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (CloseableIterator<NamedCsvRecord> it = NamedCsvReader.from(crb.build(csr)).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertThat(csr.isClosed()).isTrue();

        csr = supp.get();
        try (Stream<NamedCsvRecord> stream = NamedCsvReader.from(crb.build(csr)).stream()) {
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
            NamedCsvReader.from(crb.build("a,b,c\n1,2,3\n4,5,6")).spliterator();

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
        final NamedCsvReader csvReader = NamedCsvReader.from(crb
            .build(new UncloseableReader(new StringReader("foo"))));

        assertThatThrownBy(() -> csvReader.stream().close())
            .isInstanceOf(UncheckedIOException.class)
            .hasMessage("java.io.IOException: Cannot close");
    }

    // test helpers

    private NamedCsvReader parse(final String data) {
        return NamedCsvReader.from(crb.build(data));
    }

    private List<NamedCsvRecord> readAll(final String data) {
        return parse(data).stream().collect(Collectors.toList());
    }

}
