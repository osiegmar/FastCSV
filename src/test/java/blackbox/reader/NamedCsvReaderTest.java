package blackbox.reader;

import static blackbox.Util.asArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
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
import de.siegmar.fastcsv.reader.NamedCsvRow;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CloseResource"})
public class NamedCsvReaderTest {

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder();

    @Test
    public void empty() {
        final NamedCsvReader parse = parse("");
        assertArrayEquals(new String[0], parse.getHeader().toArray());
        final Iterator<NamedCsvRow> it = parse.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // toString()

    @Test
    public void readerToString() {
        assertEquals("NamedCsvReader[header=null, csvReader=CsvReader["
                + "commentStrategy=NONE, skipEmptyRows=true, errorOnDifferentFieldCount=true]]",
            crb.build("h1\nd1").toString());
    }

    @Test
    public void duplicateHeader() {
        final IllegalStateException e =
            assertThrows(IllegalStateException.class, () -> parse("a,b,a").getHeader());
        assertEquals("Duplicate header field 'a' found", e.getMessage());
    }

    @Test
    public void onlyHeader() {
        final NamedCsvReader csv = parse("foo,bar\n");
        assertArrayEquals(asArray("foo", "bar"), csv.getHeader().toArray());
        assertFalse(csv.iterator().hasNext());
        assertThrows(NoSuchElementException.class, () -> csv.iterator().next());
    }

    @Test
    public void onlyHeaderIterator() {
        final NamedCsvReader csv = parse("foo,bar\n");
        assertArrayEquals(asArray("foo", "bar"), csv.getHeader().toArray());
        assertFalse(csv.iterator().hasNext());
    }

    @Test
    public void getFieldByName() {
        assertEquals("bar", parse("foo\nbar").iterator().next().getField("foo"));
    }

    @Test
    public void getHeader() {
        assertArrayEquals(asArray("foo"), parse("foo\nbar").getHeader().toArray());

        final NamedCsvReader reader = parse("foo,bar\n1,2");
        assertArrayEquals(asArray("foo", "bar"), reader.getHeader().toArray());

        // second call
        assertArrayEquals(asArray("foo", "bar"), reader.getHeader().toArray());
    }

    @Test
    public void getHeaderEmptyRows() {
        final NamedCsvReader csv = parse("foo,bar");
        assertArrayEquals(asArray("foo", "bar"), csv.getHeader().toArray());
        final Iterator<NamedCsvRow> it = csv.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void getHeaderAfterSkippedRow() {
        final NamedCsvReader csv = parse("\nfoo,bar");
        assertArrayEquals(asArray("foo", "bar"), csv.getHeader().toArray());
        final Iterator<NamedCsvRow> it = csv.iterator();
        assertFalse(it.hasNext());
    }

    @Test
    public void getHeaderWithoutNextRowCall() {
        assertArrayEquals(asArray("foo"), parse("foo\n").getHeader().toArray());
    }

    @Test
    public void findNonExistingFieldByName() {
        final NoSuchElementException e = assertThrows(NoSuchElementException.class, () ->
            parse("foo\nfaz").iterator().next().getField("bar"));
        assertEquals("No element with name 'bar' found. Valid names are: [foo]",
            e.getMessage());
    }

    @Test
    public void toStringWithHeader() {
        final Iterator<NamedCsvRow> csvRow =
            parse("headerA,headerB,headerC\nfieldA,fieldB,fieldC\n").iterator();

        assertEquals("NamedCsvRow[originalLineNumber=2, "
                + "fieldMap={headerA=fieldA, headerB=fieldB, headerC=fieldC}]",
            csvRow.next().toString());
    }

    @Test
    public void fieldMap() {
        final Iterator<NamedCsvRow> it = parse("headerA,headerB,headerC\n"
            + "fieldA,fieldB,fieldC\n")
            .iterator();

        assertEquals("{headerA=fieldA, headerB=fieldB, headerC=fieldC}",
            it.next().getFieldMap().toString());
    }

    // line numbering

    @Test
    public void lineNumbering() {
        final Iterator<NamedCsvRow> it = crb
            .build(
                "h1,h2\n"
                    + "a,line 2\n"
                    + "b,line 3\r"
                    + "c,line 4\r\n"
                    + "d,\"line 5\rwith\r\nand\n\"\n"
                    + "e,line 9"
            ).iterator();

        NamedCsvRow row = it.next();
        assertEquals("a", row.getField("h1"));
        assertEquals(2, row.getOriginalLineNumber());

        row = it.next();
        assertEquals("b", row.getField("h1"));
        assertEquals(3, row.getOriginalLineNumber());

        row = it.next();
        assertEquals("c", row.getField("h1"));
        assertEquals(4, row.getOriginalLineNumber());

        row = it.next();
        assertEquals("d", row.getField("h1"));
        assertEquals(5, row.getOriginalLineNumber());

        row = it.next();
        assertEquals("e", row.getField("h1"));
        assertEquals(9, row.getOriginalLineNumber());

        assertFalse(it.hasNext());
    }

    // API

    @Test
    public void closeApi() throws IOException {
        final Consumer<NamedCsvRow> consumer = csvRow -> { };

        final Supplier<CloseStatusReader> supp =
            () -> new CloseStatusReader(new StringReader("h1,h2\nfoo,bar"));

        CloseStatusReader csr = supp.get();

        try (NamedCsvReader reader = crb.build(csr)) {
            reader.stream().forEach(consumer);
        }
        assertTrue(csr.isClosed());

        csr = supp.get();
        try (CloseableIterator<NamedCsvRow> it = crb.build(csr).iterator()) {
            it.forEachRemaining(consumer);
        }
        assertTrue(csr.isClosed());

        csr = supp.get();
        try (Stream<NamedCsvRow> stream = crb.build(csr).stream()) {
            stream.forEach(consumer);
        }
        assertTrue(csr.isClosed());
    }

    @Test
    public void noComments() {
        final List<NamedCsvRow> data = readAll("# comment 1\nfieldA");
        assertEquals("fieldA", data.iterator().next().getField("# comment 1"));
    }

    @Test
    public void spliterator() {
        final Spliterator<NamedCsvRow> spliterator =
            crb.build("a,b,c\n1,2,3\n4,5,6").spliterator();

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
        final NamedCsvReader csvReader = crb
            .build(new UncloseableReader(new StringReader("foo")));
        final IOException e = assertThrows(IOException.class,
            () -> csvReader.stream().close());

        assertEquals("Cannot close", e.getMessage());
    }

    // test helpers

    private NamedCsvReader parse(final String data) {
        return crb.build(data);
    }

    private List<NamedCsvRow> readAll(final String data) {
        return parse(data).stream().collect(Collectors.toList());
    }

}
