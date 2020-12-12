package blackbox.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvReaderBuilder;
import de.siegmar.fastcsv.reader.CsvRow;

public class CsvReaderTest {

    private final CsvReaderBuilder crb = CsvReader.builder();

    // null / empty input

    @Test
    public void nullInput() {
        assertThrows(NullPointerException.class, () -> parse(findBugsSafeNullInput()));
    }

    private static String findBugsSafeNullInput() {
        return null;
    }

    @Test
    public void empty() {
        final Iterator<CsvRow> it = parse("").iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    private static String[] asArray(final String... items) {
        return items;
    }

    @Test
    public void bom() {
        final byte[] bom8 = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', ',', 'b'};

        // Expecting trouble when reading BOM
        final Reader standardReader = new InputStreamReader(
            new ByteArrayInputStream(bom8), StandardCharsets.UTF_8);
        assertArrayEquals(asArray("\uFEFFa", "b"),
            crb.build(standardReader).iterator().next().getFields());

        // Reading BOM requires external support (e.g. org.apache.commons.io.input.BOMInputStream)
        final Reader bomReader = new InputStreamReader(new BOMInputStream(
            new ByteArrayInputStream(bom8)), StandardCharsets.UTF_8);
        assertArrayEquals(asArray("a", "b"),
            crb.build(bomReader).iterator().next().getFields());
    }

    // skipped rows

    @Test
    public void singleRowNoSkipEmpty() {
        crb.skipEmptyRows(false);
        assertFalse(parse("").iterator().hasNext());
    }

    @Test
    public void multipleRowsNoSkipEmpty() {
        crb.skipEmptyRows(false);
        final Iterator<CsvRow> it = parse("\n\n").iterator();

        CsvRow row = it.next();
        assertEquals(1, row.getFieldCount());
        assertEquals(1, row.getOriginalLineNumber());
        assertArrayEquals(asArray(""), row.getFields());

        row = it.next();
        assertEquals(1, row.getFieldCount());
        assertEquals(2, row.getOriginalLineNumber());
        assertArrayEquals(asArray(""), row.getFields());

        assertFalse(it.hasNext());
    }

    @Test
    public void skippedRows() {
        final List<CsvRow> csv = readAll("\n\nfoo\n\nbar\n\n");
        assertEquals(2, csv.size());

        final Iterator<CsvRow> it = csv.iterator();

        CsvRow row = it.next();
        assertEquals(3, row.getOriginalLineNumber());
        assertArrayEquals(asArray("foo"), row.getFields());

        row = it.next();
        assertEquals(5, row.getOriginalLineNumber());
        assertArrayEquals(asArray("bar"), row.getFields());
    }

    // different field count

    @Test
    public void differentFieldCountSuccess() {
        crb.errorOnDifferentFieldCount(true);

        readAll("foo\nbar");
        readAll("foo\nbar\n");

        readAll("foo,bar\nfaz,baz");
        readAll("foo,bar\nfaz,baz\n");

        readAll("foo,bar\n,baz");
        readAll(",bar\nfaz,baz");
    }

    @Test
    public void differentFieldCountFail() {
        crb.errorOnDifferentFieldCount(true);

        final UncheckedIOException e = assertThrows(UncheckedIOException.class,
            () -> readAll("foo\nbar,\"baz\nbax\""));

        assertEquals("java.io.IOException: Row 2 has 2 fields, "
            + "but first row had 1 fields", e.getMessage());
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    public void getNonExistingFieldByIndex() {
        final CsvRow csvRow = readSingleRow("foo");
        assertThrows(IndexOutOfBoundsException.class, () -> csvRow.getField(1));
    }

    // line numbering

    @Test
    public void lineNumbering() {
        final Iterator<CsvRow> it = crb
            .commentStrategy(CommentStrategy.SKIP)
            .build(new StringReader(
            "line 1\n"
                + "line 2\r"
                + "line 3\r\n"
                + "\"line 4\rwith\r\nand\n\"\n"
                + "#line 8\n"
                + "line 9"
        )).iterator();

        CsvRow row = it.next();
        assertArrayEquals(asArray("line 1"), row.getFields());
        assertEquals(1, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 2"), row.getFields());
        assertEquals(2, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 3"), row.getFields());
        assertEquals(3, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 4\rwith\r\nand\n"), row.getFields());
        assertEquals(4, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(asArray("line 9"), row.getFields());
        assertEquals(9, row.getOriginalLineNumber());

        assertFalse(it.hasNext());
    }

    // comment

    @Test
    public void comment() {
        final Iterator<CsvRow> it = crb
            .commentStrategy(CommentStrategy.READ)
            .build(new StringReader("#comment \"1\"\na,#b,c")).iterator();

        CsvRow row = it.next();
        assertTrue(row.isComment());
        assertEquals(1, row.getOriginalLineNumber());
        assertArrayEquals(asArray("comment \"1\""), row.getFields());

        row = it.next();
        assertFalse(row.isComment());
        assertEquals(2, row.getOriginalLineNumber());
        assertArrayEquals(asArray("a", "#b", "c"), row.getFields());
    }

    // to string

    @Test
    public void toStringWithoutHeader() {
        assertEquals("CsvRowImpl[originalLineNumber=1, fields=[fieldA, fieldB]]",
            readSingleRow("fieldA,fieldB\n").toString());
    }

    // buffer exceed

    @Test
    public void bufferExceed() {
        final byte[] buf = new byte[8 * 1024 * 1024];
        Arrays.fill(buf, (byte) 'a');
        buf[buf.length - 1] = (byte) ',';

        crb.build(new InputStreamReader(new ByteArrayInputStream(buf), StandardCharsets.UTF_8))
            .iterator().next();

        buf[buf.length - 1] = (byte) 'a';
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            crb.build(new InputStreamReader(new ByteArrayInputStream(buf), StandardCharsets.UTF_8))
                .iterator().next());
        assertEquals("Maximum buffer size 8388608 is not enough to read data",
            exception.getMessage());
    }

    // API

    @Test
    public void closeApi() throws IOException {
        final Consumer<CsvRow> consumer = csvRow -> { };

        final Supplier<CloseStatusReader> supp =
            () -> new CloseStatusReader(new StringReader("foo,bar"));

        CloseStatusReader csr = supp.get();
        try (CsvReader reader = crb.build(csr)) {
            reader.stream().forEach(consumer);
        }
        assertTrue(csr.isClosed());

        csr = supp.get();
        try (CloseableIterator<CsvRow> it = crb.build(csr).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertTrue(csr.isClosed());

        csr = supp.get();
        try (Stream<CsvRow> stream = crb.build(csr).stream()) {
            stream.forEach(consumer);
        }
        assertTrue(csr.isClosed());
    }

    @Test
    public void spliterator() {
        final Spliterator<CsvRow> spliterator =
            crb.build(new StringReader("a,b,c\n1,2,3")).spliterator();

        assertNull(spliterator.trySplit());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());

        final AtomicInteger rows = new AtomicInteger();
        final AtomicInteger rows2 = new AtomicInteger();
        while (spliterator.tryAdvance(row -> rows.incrementAndGet())) {
            rows2.incrementAndGet();
        }

        assertEquals(2, rows.get());
        assertEquals(2, rows2.get());
    }

    // Coverage

    @Test
    public void closeException() {
        final CsvReader csvReader = crb.build(new UncloseableReader(new StringReader("foo")));
        final UncheckedIOException e = assertThrows(UncheckedIOException.class,
            () -> csvReader.stream().close());

        assertEquals("java.io.IOException: Cannot close", e.getMessage());
    }

    // test helpers

    private CsvReader parse(final String data) {
        return crb.build(new StringReader(data));
    }

    private CsvRow readSingleRow(final String data) {
        final List<CsvRow> lists = readAll(data);
        assertEquals(1, lists.size());
        return lists.get(0);
    }

    private List<CsvRow> readAll(final String data) {
        return parse(data)
            .stream()
            .collect(Collectors.toList());
    }

}
