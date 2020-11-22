package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.reader.CharacterConv.parse;
import static de.siegmar.fastcsv.reader.CharacterConv.print;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class GenericDataTest {

    private static final Pattern LINE_PATTERN =
        Pattern.compile("^(?<input>\\S+)(?:\\s+(?<expected>\\S+))(?:\\s+\\[(?<flags>\\w+)])?");

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void dataTest(final TestData data) {
        final String expected = print(data.getExpected());
        final String actual = print(readAll(parse(data.getInput()), data.isSkipEmptyLines()));
        assertEquals(expected, actual, () -> String.format("Error in line: '%s'", data));
    }

    static List<TestData> dataProvider() throws IOException {
        final List<TestData> data = new ArrayList<>();
        int lineNo = 0;
        try (BufferedReader r = resource()) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                final Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    final String input = matcher.group("input");
                    final String expected = matcher.group("expected");
                    final String flags = matcher.group("flags");
                    data.add(new TestData(lineNo, line, input, expected, flags));
                }
            }
        }

        return data;
    }

    private static BufferedReader resource() {
        return new BufferedReader(new InputStreamReader(
            GenericDataTest.class.getResourceAsStream("/test.txt"), StandardCharsets.UTF_8));
    }

    public static List<String[]> readAll(final String data, final boolean skipEmptyLines) {
        return CsvReader.builder()
            .skipEmptyRows(skipEmptyLines)
            .build(new StringReader(data))
            .stream()
            .map(CsvRow::getFields)
            .collect(Collectors.toList());
    }

    static class TestData {
        private final int lineNo;
        private final String line;
        private final String input;
        private final String expected;
        private final boolean skipEmptyLines;

        TestData(final int lineNo, final String line, final String input, final String expected,
                 final String flags) {
            this.lineNo = lineNo;
            this.line = line;
            this.input = input;
            this.expected = expected;
            skipEmptyLines = "skipEmptyLines".equals(flags);
        }

        public int getLineNo() {
            return lineNo;
        }

        public String getLine() {
            return line;
        }

        public String getInput() {
            return input;
        }

        public String getExpected() {
            return expected;
        }

        public boolean isSkipEmptyLines() {
            return skipEmptyLines;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", TestData.class.getSimpleName() + "[", "]")
                .add("lineNo=" + lineNo)
                .add("line='" + line + "'")
                .add("input='" + input + "'")
                .add("expected='" + expected + "'")
                .add("skipEmptyLines=" + skipEmptyLines)
                .toString();
        }
    }

}
