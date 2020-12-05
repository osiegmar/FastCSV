package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.reader.CharacterConv.parse;
import static de.siegmar.fastcsv.reader.CharacterConv.print;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class GenericDataTest {

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void dataTest(final DataProvider.TestData data) {
        final String expected = print(data.getExpected());
        final CommentStrategy commentStrategy = data.isReadComments()
            ? CommentStrategy.READ
            : data.isSkipComments() ? CommentStrategy.SKIP : CommentStrategy.NONE;
        final String actual = print(readAll(parse(data.getInput()), data.isSkipEmptyLines(),
            commentStrategy));
        assertEquals(expected, actual, () -> String.format("Error in line: '%s'", data));
    }

    static List<DataProvider.TestData> dataProvider() throws IOException {
        return DataProvider.loadTestData("/test.txt");
    }

    private static BufferedReader resource() {
        return new BufferedReader(new InputStreamReader(
            GenericDataTest.class.getResourceAsStream("/test.txt"), StandardCharsets.UTF_8));
    }

    public static List<String[]> readAll(final String data, final boolean skipEmptyLines,
                                         final CommentStrategy commentStrategy) {
        return CsvReader.builder()
            .skipEmptyRows(skipEmptyLines)
            .commentCharacter(';')
            .commentStrategy(commentStrategy)
            .build(new StringReader(data))
            .stream()
            .map(CsvRow::getFields)
            .collect(Collectors.toList());
    }

}
