package blackbox.reader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
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

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.MalformedCsvException;

@SuppressWarnings({
    "checkstyle:ClassFanOutComplexity",
    "PMD.AvoidDuplicateLiterals",
    "PMD.CloseResource"
})
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

    static Stream<Arguments> provideBuilderForMisconfiguration() {
        return Stream.of(
            Arguments.of(CsvReader.builder().quoteCharacter(',')),
            Arguments.of(CsvReader.builder().commentCharacter(',')),
            Arguments.of(CsvReader.builder().quoteCharacter('#').commentCharacter('#'))
        );
    }

    @Test
    public void empty() {
        final Iterator<CsvRow> it = crb.build("").iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void immutableResponse() {
        final List<String> fields = crb.build("foo").iterator().next().getFields();
        assertThrows(UnsupportedOperationException.class, () -> fields.add("bar"));
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
        assertEquals(Collections.singletonList(""), row.getFields());

        row = it.next();
        assertTrue(row.isEmpty());
        assertEquals(1, row.getFieldCount());
        assertEquals(2, row.getOriginalLineNumber());
        assertEquals(Collections.singletonList(""), row.getFields());

        row = it.next();
        assertFalse(row.isEmpty());
        assertEquals(1, row.getFieldCount());
        assertEquals(3, row.getOriginalLineNumber());
        assertEquals(Collections.singletonList("a"), row.getFields());

        assertFalse(it.hasNext());
    }

    @Test
    public void skippedRows() {
        final List<CsvRow> csv = readAll("\n\nfoo\n\nbar\n\n");
        assertEquals(2, csv.size());

        final Iterator<CsvRow> it = csv.iterator();

        CsvRow row = it.next();
        assertEquals(3, row.getOriginalLineNumber());
        assertEquals(Collections.singletonList("foo"), row.getFields());

        row = it.next();
        assertEquals(5, row.getOriginalLineNumber());
        assertEquals(Collections.singletonList("bar"), row.getFields());
    }

    // different field count

    @Test
    public void differentFieldCountSuccess() {
        crb.errorOnDifferentFieldCount(true);

        assertDoesNotThrow(() -> readAll("foo\nbar"));
        assertDoesNotThrow(() -> readAll("foo\nbar\n"));

        assertDoesNotThrow(() -> readAll("foo,bar\nfaz,baz"));
        assertDoesNotThrow(() -> readAll("foo,bar\nfaz,baz\n"));

        assertDoesNotThrow(() -> readAll("foo,bar\n,baz"));
        assertDoesNotThrow(() -> readAll(",bar\nfaz,baz"));
    }

    @Test
    public void differentFieldCountFail() {
        crb.errorOnDifferentFieldCount(true);

        final MalformedCsvException e = assertThrows(MalformedCsvException.class,
            () -> readAll("foo\nbar,\"baz\nbax\""));

        assertEquals("Row 2 has 2 fields, but first row had 1 fields", e.getMessage());
    }

    // field by index

    @Test
    @SuppressWarnings("CheckReturnValue")
    public void getNonExistingFieldByIndex() {
        assertThrows(IndexOutOfBoundsException.class, () ->
            spotbugs(readSingleRow("foo").getField(1)));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
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
        assertEquals(Collections.singletonList("line 1"), row.getFields());
        assertEquals(1, row.getOriginalLineNumber());

        row = it.next();
        assertEquals(Collections.singletonList("line 2"), row.getFields());
        assertEquals(2, row.getOriginalLineNumber());

        row = it.next();
        assertEquals(Collections.singletonList("line 3"), row.getFields());
        assertEquals(3, row.getOriginalLineNumber());

        row = it.next();
        assertEquals(Collections.singletonList("line 4\rwith\r\nand\n"), row.getFields());
        assertEquals(4, row.getOriginalLineNumber());

        row = it.next();
        assertEquals(Collections.singletonList("line 9"), row.getFields());
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
        assertEquals(Collections.singletonList("comment \"1\""), row.getFields());

        row = it.next();
        assertFalse(row.isComment());
        assertEquals(2, row.getOriginalLineNumber());
        assertEquals(Arrays.asList("a", "#b", "c"), row.getFields());
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
        final char[] buf = new char[8 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        buf[buf.length - 1] = ',';

        crb.build(new CharArrayReader(buf)).iterator().next();

        buf[buf.length - 1] = (byte) 'X';
        final UncheckedIOException exception = assertThrows(UncheckedIOException.class, () ->
            crb.build(new CharArrayReader(buf)).iterator().next());
        assertEquals("IOException when reading first record", exception.getMessage());

        assertEquals("Maximum buffer size 8388608 is not enough to read data of a single field. "
                + "Typically, this happens if quotation started but did not end within this buffer's "
                + "maximum boundary.",
            exception.getCause().getMessage());
    }

    @Test
    public void bufferExceedSubsequentRecord() {
        final char[] buf = new char[8 * 1024 * 1024];
        Arrays.fill(buf, 'X');
        final String s = "a,b,c\n\"";
        System.arraycopy(s.toCharArray(), 0, buf, 0, s.length());

        final CloseableIterator<CsvRow> iterator = crb.build(new CharArrayReader(buf)).iterator();

        iterator.next();

        final UncheckedIOException exception = assertThrows(UncheckedIOException.class, iterator::next);
        assertEquals("IOException when reading record that started in line 2", exception.getMessage());

        assertEquals("Maximum buffer size 8388608 is not enough to read data of a single field. "
                + "Typically, this happens if quotation started but did not end within this buffer's "
                + "maximum boundary.",
            exception.getCause().getMessage());
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
    public void closeStringNoException() {
        assertDoesNotThrow(() -> crb.build("foo").close());
    }

    @Test
    public void spliterator() {
        final Spliterator<CsvRow> spliterator =
            crb.build("a,b,c\n1,2,3").spliterator();

        assertNull(spliterator.trySplit());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
        assertEquals(Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL
            | Spliterator.IMMUTABLE, spliterator.characteristics());

        final AtomicInteger rows = new AtomicInteger();
        final AtomicInteger rows2 = new AtomicInteger();
        while (spliterator.tryAdvance(row -> rows.incrementAndGet())) {
            rows2.incrementAndGet();
        }

        assertEquals(2, rows.get());
        assertEquals(2, rows2.get());
    }

    @Test
    public void parallelDistinct() {
        assertEquals(2, crb.build("foo\nfoo").stream().parallel().distinct().count());
    }

    // Coverage

    @Test
    public void closeException() {
        final CsvReader csvReader = crb.build(new UncloseableReader(new StringReader("foo")));
        final UncheckedIOException e = assertThrows(UncheckedIOException.class,
            () -> csvReader.stream().close());

        assertEquals("java.io.IOException: Cannot close", e.getMessage());
    }

    @Test
    public void unreadable() {
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () ->
            crb.build(new UnreadableReader()).iterator().next());

        assertEquals("IOException when reading first record", e.getMessage());
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
