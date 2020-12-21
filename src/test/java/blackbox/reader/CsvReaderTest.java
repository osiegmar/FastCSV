package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import blackbox.Util;
import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class CsvReaderTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @ParameterizedTest
    @ValueSource(chars = {'\r', '\n'})
    public void configBuilder(final char c) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            CsvReader.builder().fieldSeparator(c).build("foo"));
        assertEquals("fieldSeparator must not be a newline char", e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
            CsvReader.builder().quoteCharacter(c).build("foo"));
        assertEquals("quoteCharacter must not be a newline char", e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
            CsvReader.builder().commentCharacter(c).build("foo"));
        assertEquals("commentCharacter must not be a newline char", e3.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideBuilderForMisconfiguration")
    public void configReader(final CsvReader.CsvReaderBuilder builder) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            builder.build("foo"));
        assertTrue(e.getMessage().contains("Control characters must differ"));
    }

    private static Stream<Arguments> provideBuilderForMisconfiguration() {
        return Stream.of(
            Arguments.of(CsvReader.builder().fieldSeparator(',').quoteCharacter(',')),
            Arguments.of(CsvReader.builder().fieldSeparator(',').commentCharacter(',')),
            Arguments.of(CsvReader.builder().quoteCharacter(',').commentCharacter(','))
        );
    }

    @Test
    public void empty() {
        final Iterator<CsvRow> it = crb.build("").iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // toString()

    @Test
    public void readerToString() {
        assertEquals("CsvReader[commentStrategy=NONE, skipEmptyRows=true, "
            + "errorOnDifferentFieldCount=false]", crb.build("").toString());
    }

    // skipped rows

    @Test
    public void singleRowNoSkipEmpty() {
        crb.skipEmptyRows(false);
        assertFalse(crb.build("").iterator().hasNext());
    }

    @Test
    public void multipleRowsNoSkipEmpty() {
        crb.skipEmptyRows(false);
        final Iterator<CsvRow> it = crb.build("\n\na").iterator();

        CsvRow row = it.next();
        assertTrue(row.isEmpty());
        assertEquals(1, row.getFieldCount());
        assertEquals(1, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray(""), row.getFields());

        row = it.next();
        assertTrue(row.isEmpty());
        assertEquals(1, row.getFieldCount());
        assertEquals(2, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray(""), row.getFields());

        row = it.next();
        assertFalse(row.isEmpty());
        assertEquals(1, row.getFieldCount());
        assertEquals(3, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray("a"), row.getFields());

        assertFalse(it.hasNext());
    }

    @Test
    public void skippedRows() {
        final List<CsvRow> csv = readAll("\n\nfoo\n\nbar\n\n");
        assertEquals(2, csv.size());

        final Iterator<CsvRow> it = csv.iterator();

        CsvRow row = it.next();
        assertEquals(3, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray("foo"), row.getFields());

        row = it.next();
        assertEquals(5, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray("bar"), row.getFields());
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
        assertThrows(IndexOutOfBoundsException.class, () ->
            spotbugs(readSingleRow("foo").getField(1)));
    }

    private void spotbugs(final String foo) {
        // Prevent RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    }

    // line numbering

    @Test
    public void lineNumbering() {
        final Iterator<CsvRow> it = crb
            .commentStrategy(CommentStrategy.SKIP)
            .build(
                "line 1\n"
                    + "line 2\r"
                    + "line 3\r\n"
                    + "\"line 4\rwith\r\nand\n\"\n"
                    + "#line 8\n"
                    + "line 9"
            ).iterator();

        CsvRow row = it.next();
        assertArrayEquals(Util.asArray("line 1"), row.getFields());
        assertEquals(1, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(Util.asArray("line 2"), row.getFields());
        assertEquals(2, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(Util.asArray("line 3"), row.getFields());
        assertEquals(3, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(Util.asArray("line 4\rwith\r\nand\n"), row.getFields());
        assertEquals(4, row.getOriginalLineNumber());

        row = it.next();
        assertArrayEquals(Util.asArray("line 9"), row.getFields());
        assertEquals(9, row.getOriginalLineNumber());

        assertFalse(it.hasNext());
    }

    // comment

    @Test
    public void comment() {
        final Iterator<CsvRow> it = crb
            .commentStrategy(CommentStrategy.READ)
            .build("#comment \"1\"\na,#b,c").iterator();

        CsvRow row = it.next();
        assertTrue(row.isComment());
        assertEquals(1, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray("comment \"1\""), row.getFields());

        row = it.next();
        assertFalse(row.isComment());
        assertEquals(2, row.getOriginalLineNumber());
        assertArrayEquals(Util.asArray("a", "#b", "c"), row.getFields());
    }

    // to string

    @Test
    public void toStringWithoutHeader() {
        assertEquals("CsvRow[originalLineNumber=1, fields=[fieldA, fieldB], comment=false]",
            readSingleRow("fieldA,fieldB\n").toString());
    }

    // buffer exceed

    @Test
    public void bufferExceed() {
        final byte[] buf = new byte[8 * 1024 * 1024];
        Arrays.fill(buf, (byte) 'a');
        buf[buf.length - 1] = (byte) ',';

        crb.build(new InputStreamReader(new ByteArrayInputStream(buf), UTF_8))
            .iterator().next();

        buf[buf.length - 1] = (byte) 'a';
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            crb.build(new InputStreamReader(new ByteArrayInputStream(buf), UTF_8))
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
            crb.build("a,b,c\n1,2,3").spliterator();

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

    private CsvRow readSingleRow(final String data) {
        final List<CsvRow> lists = readAll(data);
        assertEquals(1, lists.size());
        return lists.get(0);
    }

    private List<CsvRow> readAll(final String data) {
        return crb.build(data)
            .stream()
            .collect(Collectors.toList());
    }

}
