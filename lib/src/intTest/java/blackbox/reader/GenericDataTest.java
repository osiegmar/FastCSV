package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;
import specreader.CheckVariant;
import specreader.CheckVariantWrapper;
import specreader.TestSpecRepository;
import specreader.spec.TestSpec;
import specreader.spec.TestSpecSettings;

/// This test uses a set of test specs to verify the correctness of the CSV parser.
/// The test specs cover all relevant parser branches of FastCSV.
///
/// It runs once for the RFC 4180 optimized strict parser and once for the relaxed parser
/// (selected via the `de.siegmar.fastcsv.relaxed` system property).
///
/// The test specs can be reused for other CSV parsers as well.
@ParameterizedClass
@EnumSource(GenericDataTest.Mode.class)
class GenericDataTest {

    @Parameter
    private Mode mode;

    @BeforeParameterizedClassInvocation
    static void setMode(final Mode mode) {
        // Set the property explicitly for both modes so a strict invocation never silently
        // inherits a relaxed parser from an externally set property.
        System.setProperty("de.siegmar.fastcsv.relaxed", String.valueOf(mode == Mode.RELAXED));
    }

    @AfterParameterizedClassInvocation
    static void clearMode() {
        System.clearProperty("de.siegmar.fastcsv.relaxed");
    }

    /// Guards the parser-selection toggle itself: strict and relaxed parsers produce identical
    /// records for the RFC-compliant specs, so {@link #dataTest} alone cannot detect if a broken
    /// toggle silently ran the wrong parser for a whole mode. This asserts the intended parser is
    /// actually exercised. Runs once per mode (it is a plain @Test inside a @ParameterizedClass).
    @Test
    void exercisesExpectedParser() {
        final String expectedParser = mode == Mode.RELAXED ? "RelaxedCsvParser" : "StrictCsvParser";
        assertThat(CsvReader.builder().ofCsvRecord("a,b").toString())
            .contains("parser=" + expectedParser);
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("dataProvider")
    void dataTest(final String name, final TestSpec testSpec, final CheckVariant checkVariant) {
        if (mode == Mode.RELAXED && testSpec.settings().exceptionAllowed()) {
            // The relaxed parser may reject inputs the spec marks as exception-allowed: a thrown
            // CsvParseException is an acceptable outcome; otherwise the parsed records must match.
            try {
                assertRecords(name, testSpec, checkVariant);
            } catch (final CsvParseException ignored) {
                // tolerated: the spec explicitly allows an exception for this case
            }
        } else {
            assertRecords(name, testSpec, checkVariant);
        }
    }

    private void assertRecords(final String name, final TestSpec testSpec, final CheckVariant checkVariant) {
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

    /// Adapter method to actually parse the CSV records and return them as library-independent data structure.
    ///
    /// @param input    the CSV input (virtual file content including line breaks)
    /// @param settings the test spec settings
    /// @return the parsed CSV records
    @SuppressWarnings("removal")
    private List<List<String>> parseCsvRecords(final TestSpecSettings settings, final String input) {
        final CommentStrategy commentStrategy = switch (settings.commentMode()) {
            case NONE -> CommentStrategy.NONE;
            case READ -> CommentStrategy.READ;
            case SKIP -> CommentStrategy.SKIP;
        };

        final CsvReader.CsvReaderBuilder builder = CsvReader.builder()
            .commentStrategy(commentStrategy)
            .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
            .missingFieldStrategy(FieldMismatchStrategy.IGNORE)
            .skipEmptyLines(settings.skipEmptyLines());

        if (mode == Mode.STRICT) {
            builder.allowExtraCharsAfterClosingQuote(true);
        }

        return builder
            .ofCsvRecord(input)
            .stream()
            .map(CsvRecord::getFields)
            .toList();
    }

    enum Mode {
        STRICT, RELAXED
    }

}
