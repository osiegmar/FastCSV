package de.siegmar.fastcsv.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DataProvider {

    private static final Pattern LINE_PATTERN =
        Pattern.compile("^(?<input>\\S+)(?:\\s+(?<expected>\\S+))(?:\\s+\\[(?<flags>\\w+)])?");

    private DataProvider() {
    }

    static List<TestData> loadTestData(final String name) throws IOException {
        final List<TestData> data = new ArrayList<>();
        try (BufferedReader r = resource(name)) {
            String line;
            int lineNo = 0;
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

    private static BufferedReader resource(final String name) {
        return new BufferedReader(new InputStreamReader(
            DataProvider.class.getResourceAsStream(name), StandardCharsets.UTF_8));
    }

    public static class TestData {

        private final int lineNo;
        private final String line;
        private final String input;
        private final String expected;
        private final boolean skipEmptyLines;
        private final boolean readComments;
        private final boolean skipComments;

        TestData(final int lineNo, final String line, final String input, final String expected,
                 final String flags) {
            this.lineNo = lineNo;
            this.line = line;
            this.input = input;
            this.expected = expected;
            skipEmptyLines = "skipEmptyLines".equals(flags);
            readComments = "readComments".equals(flags);
            skipComments = "skipComments".equals(flags);
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

        public boolean isReadComments() {
            return readComments;
        }

        public boolean isSkipComments() {
            return skipComments;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", DataProvider.TestData.class.getSimpleName() + "[", "]")
                .add("lineNo=" + lineNo)
                .add("line='" + line + "'")
                .add("input='" + input + "'")
                .add("expected='" + expected + "'")
                .add("skipEmptyLines=" + skipEmptyLines)
                .add("readComments=" + readComments)
                .add("skipComments=" + skipComments)
                .toString();
        }

    }

}
