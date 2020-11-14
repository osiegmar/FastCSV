/*
 * Copyright 2020 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.reader.CharacterConv.parse;
import static de.siegmar.fastcsv.reader.CharacterConv.print;
import static de.siegmar.fastcsv.reader.TestUtil.readAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class GenericDataTest {

    private static final Pattern LINE_PATTERN =
        Pattern.compile("^(?<source>[^ ]+)([ ]+(?<expected>[^ ]+))*");

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void dataTest(final TestData data) {
        final String expected = print(data.getExpected());
        final String actual = print(readAll(parse(data.getSource())));
        assertEquals(expected, actual, () -> "Error in line: '" + data.getLine() + "'");
    }

    static List<TestData> dataProvider() throws IOException {
        final List<TestData> data = new ArrayList<>();
        try (BufferedReader r = resource()) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                final Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    final String source = matcher.group("source");
                    final String expected = matcher.group("expected");
                    data.add(new TestData(line, source, expected != null ? expected : source));
                }
            }
        }

        return data;
    }

    private static BufferedReader resource() {
        return new BufferedReader(new InputStreamReader(
            GenericDataTest.class.getResourceAsStream("/test.txt"), StandardCharsets.UTF_8));
    }

    static class TestData {
        private final String line;
        private final String source;
        private final String expected;

        TestData(final String line, final String source, final String expected) {
            this.line = line;
            this.source = source;
            this.expected = expected;
        }

        public String getLine() {
            return line;
        }

        public String getSource() {
            return source;
        }

        public String getExpected() {
            return expected;
        }
    }

}
