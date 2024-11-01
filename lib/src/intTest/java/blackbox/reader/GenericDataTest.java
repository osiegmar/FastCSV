package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import specreader.CheckVariant;
import specreader.CheckVariantWrapper;
import specreader.TestSpecRepository;
import specreader.spec.TestSpec;
import specreader.spec.TestSpecSettings;

/**
 * This test uses a set of test specs to verify the correctness of the CSV parser.
 * The test specs cover all relevant parser branches of FastCSV.
 * <p>
 * The test specs can be reused for other CSV parsers as well.
 */
class GenericDataTest {

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("dataProvider")
    void dataTest(final String name, final TestSpec testSpec, final CheckVariant checkVariant) {
        final List<List<String>> actual =
            parseCsvRecords(testSpec.settings(), checkVariant.variant().data());

        assertThat(actual)
            .withFailMessage(
                """
                    Test     : %s
                    Expected : %s
                    Actual   : %s
                    Spec     : %s
                    Test     : %s
                    """, name, checkVariant.records(), actual, testSpec.description(), checkVariant.description())
            .isEqualTo(checkVariant.records());
    }

    static Stream<Arguments> dataProvider() {
        return TestSpecRepository.loadTests(Path.of("src/intTest/resources/spec"))
            .map(spec -> Arguments.of(buildName(spec), spec.testSpecFile().spec(), spec.testSpecCheck()));
    }

    private static String buildName(final CheckVariantWrapper spec) {
        final var formattedInput = spec.testSpecCheck().variant().data()
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\f", "\\f");

        return "`%s` from Test \"%s\" (`%s` in %s)".formatted(
            formattedInput,
            spec.testSpecFile().spec().name(),
            spec.testSpecCheck().variant().orig(),
            String.valueOf(spec.testSpecFile().file().getFileName()));
    }

    /**
     * Adapter method to actually parse the CSV records and return them as library-independent data structure.
     *
     * @param input    the CSV input (virtual file content including line breaks)
     * @param settings the test spec settings
     * @return the parsed CSV records
     */
    private static List<List<String>> parseCsvRecords(final TestSpecSettings settings, final String input) {

        final CommentStrategy commentStrategy = switch (settings.commentMode()) {
            case NONE -> CommentStrategy.NONE;
            case READ -> CommentStrategy.READ;
            case SKIP -> CommentStrategy.SKIP;
        };

        return CsvReader.builder()
            .commentStrategy(commentStrategy)
            .skipEmptyLines(settings.skipEmptyLines())
            .ofCsvRecord(input)
            .stream()
            .map(CsvRecord::getFields)
            .toList();
    }

}
