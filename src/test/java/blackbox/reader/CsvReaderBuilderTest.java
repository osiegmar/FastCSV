package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

@SuppressWarnings("PMD.CloseResource")
class CsvReaderBuilderTest {

    private static final String DATA = "foo,bar\n";
    private static final List<String> EXPECTED = Arrays.asList("foo", "bar");

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @ParameterizedTest
    @NullSource
    void nullInput(final String text) {
        assertThrows(NullPointerException.class, () -> crb.build(text));
    }

    @Test
    void fieldSeparator() {
        final Iterator<CsvRow> it = crb.fieldSeparator(';')
            .build("foo,bar;baz").iterator();
        assertEquals(Arrays.asList("foo,bar", "baz"), it.next().getFields());
    }

    @Test
    void quoteCharacter() {
        final Iterator<CsvRow> it = crb.quoteCharacter('_')
            .build("_foo \", __ bar_,foo \" bar").iterator();
        assertEquals(Arrays.asList("foo \", _ bar", "foo \" bar"), it.next().getFields());
    }

    @Test
    void commentSkip() {
        final Iterator<CsvRow> it = crb.commentCharacter(';').commentStrategy(CommentStrategy.SKIP)
            .build("#foo\n;bar\nbaz").iterator();
        assertEquals(Collections.singletonList("#foo"), it.next().getFields());
        assertEquals(Collections.singletonList("baz"), it.next().getFields());
    }

    @Test
    void builderToString() {
        assertEquals("CsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
            + "commentStrategy=NONE, commentCharacter=#, skipEmptyRows=true, "
            + "errorOnDifferentFieldCount=false]", crb.toString());
    }

    @Test
    void reader() {
        final List<CsvRow> list = crb
            .build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, list.get(0).getFields());
    }

    @Test
    void string() {
        final List<CsvRow> list = crb.build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, list.get(0).getFields());
    }

    @Test
    void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        final List<CsvRow> list;
        try (Stream<CsvRow> stream = crb.build(file).stream()) {
            list = stream.collect(Collectors.toList());
        }

        assertEquals(EXPECTED, list.get(0).getFields());
    }

    @Test
    void chained() {
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
