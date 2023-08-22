package blackbox.reader;

import static blackbox.reader.CharacterConv.parse;
import static blackbox.reader.CharacterConv.print;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

class GenericDataTest {

    @ParameterizedTest
    @MethodSource("dataProvider")
    void dataTest(final DataProvider.GenericTestData data) {
        final String expected = print(data.getExpected());
        final CommentStrategy commentStrategy = data.isReadComments()
            ? CommentStrategy.READ
            : data.isSkipComments() ? CommentStrategy.SKIP : CommentStrategy.NONE;
        final String actual = print(readAll(parse(data.getInput()), data.isSkipEmptyLines(),
            commentStrategy));

        assertThat(actual)
            .withFailMessage(() -> String.format("Error in line %d: expected='%s', actual='%s'",
                data.getLineNo(), expected, actual))
            .isEqualTo(expected);
    }

    static List<DataProvider.GenericTestData> dataProvider() throws IOException {
        return DataProvider.loadTestData("/test.txt");
    }

    public static List<List<String>> readAll(final String data, final boolean skipEmptyLines,
                                             final CommentStrategy commentStrategy) {
        return CsvReader.builder()
            .skipEmptyRecords(skipEmptyLines)
            .commentCharacter(';')
            .commentStrategy(commentStrategy)
            .build(data)
            .stream()
            .map(CsvRecord::fields)
            .collect(Collectors.toList());
    }

}
