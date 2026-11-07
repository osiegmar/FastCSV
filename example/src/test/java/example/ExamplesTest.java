package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/// Runs every example and compares its output with `src/test/resources/expected/<example>.txt`.
///
/// Run `./gradlew :example:test -PupdateExpected` to rewrite the expected files after an intended change.
@SuppressWarnings("PMD.CloseResource")
class ExamplesTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path EXPECTED = Path.of("src/test/resources/expected");
    private static final String SOURCE_SUFFIX = ".java";

    private static final Pattern TEMP_PATH = Pattern.compile(
        Pattern.quote(Path.of(System.getProperty("java.io.tmpdir")).toString()) + "[/\\\\]fastcsv\\d+");
    private static final String TEMP_PATH_PLACEHOLDER = "<tmp>";

    /// `ExampleIndexedCsvReader` writes its test file for a fixed time, so the number of records varies.
    private static final Pattern PREPARED_RECORDS = Pattern.compile("Temporary test file with ([\\d,]+) records");

    /// Line numbers and frames of a printed stack trace change with the code and the JDK.
    private static final Pattern STACK_FRAME = Pattern.compile("(?m)^\\t(at |\\.\\.\\. ).*\\n");

    static List<String> examples() throws IOException {
        try (var files = Files.list(SOURCES)) {
            return files
                .map(file -> SOURCES.relativize(file).toString())
                .map(name -> name.substring(0, name.length() - SOURCE_SUFFIX.length()))
                .sorted()
                .toList();
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    void printsExpectedOutput(final String example) throws Exception {
        final String output = normalize(run(example));

        switch (example) {
            case "ExampleCsvReaderWithCustomCallbackHandler" -> assertMapsAllRecords(output);
            case "ExampleIndexedCsvReader" -> assertIndexesAllRecords(output);
            default -> assertExpectedOutput(example, output);
        }
    }

    /// The durations and the file size vary, the number of mapped records does not.
    private static void assertMapsAllRecords(final String output) {
        assertThat(count(output, "Duration to map 1,000,000 records: ")).isEqualTo(3);
    }

    private static void assertIndexesAllRecords(final String output) {
        final var prepared = PREPARED_RECORDS.matcher(output);
        assertThat(prepared.find()).isTrue();

        final String records = prepared.group(1);
        assertThat(count(output, "Indexed " + records + " records\n")).isEqualTo(3);
        assertThat(output).contains("startingLineNumber=" + records.replace(",", "") + ",");
    }

    private static long count(final String output, final String text) {
        return Pattern.compile(text, Pattern.LITERAL).matcher(output).results().count();
    }

    private static void assertExpectedOutput(final String example, final String output) throws IOException {
        final Path expected = EXPECTED.resolve(example + ".txt");
        if (Boolean.getBoolean("updateExpected")) {
            Files.writeString(expected, output);
        }
        assertThat(output).isEqualTo(Files.readString(expected));
    }

    private static String run(final String example) throws Exception {
        final var clazz = Class.forName(example);
        final var constructor = clazz.getDeclaredConstructor();
        final var main = clazz.getDeclaredMethod("main");
        constructor.setAccessible(true);
        main.setAccessible(true);

        final PrintStream originalOut = System.out;
        final var out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            main.invoke(constructor.newInstance());
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String normalize(final String output) {
        final String withoutFrames = STACK_FRAME.matcher(output.replace("\r\n", "\n")).replaceAll("");
        return TEMP_PATH.matcher(withoutFrames).replaceAll(TEMP_PATH_PLACEHOLDER)
            .replace(TEMP_PATH_PLACEHOLDER + "\\", TEMP_PATH_PLACEHOLDER + "/");
    }

}
