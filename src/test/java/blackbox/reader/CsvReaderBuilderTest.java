package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import blackbox.Util;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

public class CsvReaderBuilderTest {

    private static final String DATA = "foo,bar\n";
    private static final String EXPECTED = "[foo, bar]";

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @ParameterizedTest
    @NullSource
    public void nullInput(final String text) {
        assertThrows(NullPointerException.class, () -> crb.build(text));
    }

    @Test
    public void fieldSeparator() {
        final Iterator<CsvRow> it = crb.fieldSeparator(';')
            .build("foo,bar;baz").iterator();
        assertArrayEquals(Util.asArray("foo,bar", "baz"), it.next().getFields());
    }

    @Test
    public void quoteCharacter() {
        final Iterator<CsvRow> it = crb.quoteCharacter('_')
            .build("_foo \", __ bar_,foo \" bar").iterator();
        assertArrayEquals(Util.asArray("foo \", _ bar", "foo \" bar"), it.next().getFields());
    }

    @Test
    public void commentSkip() {
        final Iterator<CsvRow> it = crb.commentCharacter(';').commentStrategy(CommentStrategy.SKIP)
            .build("#foo\n;bar\nbaz").iterator();
        assertArrayEquals(Util.asArray("#foo"), it.next().getFields());
        assertArrayEquals(Util.asArray("baz"), it.next().getFields());
    }

    @Test
    public void builderToString() {
        assertEquals("CsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
            + "commentStrategy=NONE, commentCharacter=#, skipEmptyRows=true, "
            + "errorOnDifferentFieldCount=false]", crb.toString());
    }

    @Test
    public void reader() throws IOException {
        final List<CsvRow> list = crb
            .build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, Arrays.toString(list.get(0).getFields()));
    }

    @Test
    public void string() throws IOException {
        final List<CsvRow> list = crb.build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, Arrays.toString(list.get(0).getFields()));
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        final List<CsvRow> list;
        try (Stream<CsvRow> stream = crb.build(file, UTF_8).stream()) {
            list = stream.collect(Collectors.toList());
        }

        assertEquals(EXPECTED, Arrays.toString(list.get(0).getFields()));
    }

    @Test
    public void chained() {
        final CsvReader reader = CsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .commentStrategy(CommentStrategy.NONE)
            .commentCharacter('#')
            .skipEmptyRows(true)
            .errorOnDifferentFieldCount(false)
            .build("foo");

        assertNotNull(reader);
    }

}
