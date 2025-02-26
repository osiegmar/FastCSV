package specreader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import specreader.spec.TestSpec;
import specreader.spec.TestSpecCheck;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestSpecRepository {

    private static final ObjectReader OR = new ObjectMapper(new YAMLFactory())
        .readerFor(TestSpec.class);

    private TestSpecRepository() {
        // utility class
    }

    public static Stream<CheckWrapper> loadChecks(final Path specPath) {
        return loadTestSpecs(specPath)
            .flatMap(TestSpecRepository::flattenChecks);
    }

    public static Stream<CheckVariantWrapper> loadTests(final Path specPath) {
        return loadTestSpecs(specPath)
            .flatMap(TestSpecRepository::flattenCheckVariants);
    }

    public static Stream<TestSpecFile> loadTestSpecs(final Path specPath) {
        try {
            return Files.list(specPath)
                .map(TestSpecRepository::loadTestSpec);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<CheckWrapper> flattenChecks(final TestSpecFile specFile) {
        return specFile.spec().checks().stream()
            .map(check -> new CheckWrapper(specFile, check));
    }

    public static Stream<CheckVariantWrapper> flattenCheckVariants(final TestSpecFile specFile) {
        return specFile.spec().checks().stream()
            .flatMap(TestSpecRepository::wrapVariant)
            .map(check -> new CheckVariantWrapper(specFile, check));
    }

    public static Stream<CheckVariant> wrapVariant(final TestSpecCheck check) {
        return variantsOf(check.input()).stream()
            .map(v -> new CheckVariant(v, check.description(), modifyRecord(check.records(), v)));
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static List<List<String>> modifyRecord(final List<List<String>> records, final Variant v) {
        final List<List<String>> ret = new ArrayList<>(records.size());
        for (final var record : records) {
            final List<String> modified = new ArrayList<>(record.size());
            for (final var field : record) {
                modified.add(replace(field, v.replacements()));
            }
            ret.add(modified);
        }

        return ret;
    }

    private static String replace(final String field, final Set<Pair> replacements) {
        String ret = field;
        for (final Pair replacement : replacements) {
            ret = ret.replace(replacement.placeholder(), replacement.replacement());
        }
        return ret;
    }

    public static Set<Variant> variantsOf(final String data) {
        // replace characters with only one variant
        final var quoteCorrected = data
            .replace("␊", "\n")
            .replace("␍", "\r")
            .replace("␤", "\r\n");

        final var variants = Set.of(new Variant(data, quoteCorrected, Set.of()));

        // return if no multi-variant characters are present
        if (!quoteCorrected.contains("⏎") && !quoteCorrected.contains("␠")) {
            return variants;
        }

        // replace characters with multiple variants
        return buildVariant(
            buildVariant(variants, "⏎", Set.of("\n", "\r\n", "\r")),
            "␠", Set.of(" ", "\t", "\f"));
    }

    private static Set<Variant> buildVariant(final Set<Variant> variants,
                                             final String placeholder, final Set<String> replacements) {
        final Set<Variant> ret = new LinkedHashSet<>();
        for (final var variant : variants) {
            if (variant.data().contains(placeholder)) {
                replacements.stream()
                    .map(replacement -> variant.extend(placeholder, replacement))
                    .forEach(ret::add);
            } else {
                ret.add(variant);
            }
        }
        return ret;
    }

    private static TestSpecFile loadTestSpec(final Path file) {
        try {
            return new TestSpecFile(file, OR.readValue(file.toFile()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
