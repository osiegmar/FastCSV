package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

public class NamedCsvReaderBuilderTest {

    private static final String DATA = "header1,header2\nfoo,bar\n";
    private static final String EXPECTED = "{header1=foo, header2=bar}";

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder();

    @ParameterizedTest
    @NullSource
    public void nullInput(final String text) {
        assertThrows(NullPointerException.class, () -> crb.build(text));
    }

    @Test
    public void fieldSeparator() {
        final Iterator<NamedCsvRow> it = crb.fieldSeparator(';')
            .build("h1;h2\nfoo,bar;baz").iterator();
        assertEquals("{h1=foo,bar, h2=baz}", it.next().getFieldMap().toString());
    }

    @Test
    public void quoteCharacter() {
        final Iterator<NamedCsvRow> it = crb.quoteCharacter('_')
            .build("h1,h2\n_foo \", __ bar_,foo \" bar").iterator();
        assertEquals("{h1=foo \", _ bar, h2=foo \" bar}", it.next().getFieldMap().toString());
    }

    @Test
    public void commentSkip() {
        final Iterator<NamedCsvRow> it = crb.commentCharacter(';').skipComments(true)
            .build("h1\n#foo\n;bar\nbaz").iterator();
        assertEquals("{h1=#foo}", it.next().getFieldMap().toString());
        assertEquals("{h1=baz}", it.next().getFieldMap().toString());
    }

    @Test
    public void builderToString() {
        assertEquals("NamedCsvReaderBuilder[fieldSeparator=,, quoteCharacter=\", "
            + "commentCharacter=#, skipComments=false]", crb.toString());
    }

    @Test
    public void reader() throws IOException {
        final List<NamedCsvRow> list = crb
            .build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, list.get(0).getFieldMap().toString());
    }

    @Test
    public void string() throws IOException {
        final List<NamedCsvRow> list = crb.build(DATA).stream()
            .collect(Collectors.toList());
        assertEquals(EXPECTED, list.get(0).getFieldMap().toString());
    }

    @Test
    public void path(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("fastcsv.csv");
        Files.write(file, DATA.getBytes(UTF_8));

        final List<NamedCsvRow> list;
        try (Stream<NamedCsvRow> stream = crb.build(file, UTF_8).stream()) {
            list = stream.collect(Collectors.toList());
        }

        assertEquals(EXPECTED, list.get(0).getFieldMap().toString());
    }

    @Test
    public void file(@TempDir final File tempDir) throws IOException {
        final File file = new File(tempDir, "fastcsv.csv");
        Files.write(file.toPath(), DATA.getBytes(UTF_8));

        final List<NamedCsvRow> list;
        try (Stream<NamedCsvRow> stream = crb.build(file, UTF_8).stream()) {
            list = stream.collect(Collectors.toList());
        }

        assertEquals(EXPECTED, list.get(0).getFieldMap().toString());
    }

}
