package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CsvScannerTest {

    @Test
    void indexer() {
        assertThat(indexesOf("^___\n^___", "\n"))
            .containsExactly(0, 4);
    }

    @Test
    void nullInput() {
        assertThatThrownBy(() -> scan((String) null, "\n"));
    }

    @Test
    void emptyInput() {
        assertThat(scan(new byte[0])).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void genericData(final String line, final String newLine, final int lineNo) {
        final List<Integer> actual = scan(line, newLine);
        final List<Integer> expected = indexesOf(line, newLine);

        assertThat(actual)
            .withFailMessage("Line %d: expected=%s but actual=%s for line '%s'",
                lineNo, expected, actual, line)
            .isEqualTo(expected);
    }

    static List<Arguments> genericData() throws IOException {
        final List<Arguments> list = new ArrayList<>();

        final List<String> lines = Files.readAllLines(Paths.get("src/intTest/resources/scanner.txt"));
        int lineNo = 0;
        for (final String line : lines) {
            lineNo++;
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }

            list.add(Arguments.of(line, "\n", lineNo));
            if (line.contains("$")) {
                list.add(Arguments.of(line, "\r", lineNo));
                list.add(Arguments.of(line, "\r\n", lineNo));
            }
        }

        return list;
    }

    @ParameterizedTest
    @ValueSource(ints = {8190, 8191, 8192, 8193})
    void bufferExceed(final int pos) {
        final byte[] buf = new byte[pos + 2];
        Arrays.fill(buf, (byte) 'A');

        assertThat(scan(buf))
            .containsExactly(0);

        buf[pos] = '\n';
        assertThat(scan(buf))
            .containsExactly(0, pos + 1);
    }

    @Test
    void unicode() {
        assertThat(scan("012u\n0".getBytes(StandardCharsets.UTF_8)))
            .containsExactly(0, 5);

        assertThat(scan("012ü\n0".getBytes(StandardCharsets.UTF_8)))
            .containsExactly(0, 6);
    }

    private static List<Integer> scan(final String line, final String newLine) {
        final String lineToScan = repl(line, newLine)
            .replace("^", "");

        return scan(lineToScan.getBytes(StandardCharsets.UTF_8));
    }

    private static List<Integer> scan(final byte[] data) {
        final List<Integer> positions = new ArrayList<>();

        try {
            new CsvScanner(Channels.newChannel(new ByteArrayInputStream(data)), (byte) ',', (byte) '"',
                CommentStrategy.READ, (byte) '#', p -> positions.add(p.intValue()), new StatusListener() { }).scan();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return positions;
    }

    private static String repl(final String line, final String newLine) {
        return line
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("$", newLine);
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private static List<Integer> indexesOf(final String str, final String newLine) {
        final List<Integer> indexes = new ArrayList<>();

        final String strToFindIndexes = repl(str, newLine);
        for (int index = 0; (index = strToFindIndexes.indexOf('^', index)) != -1; index++) {
            indexes.add(index - indexes.size());
        }

        return indexes;
    }

}
