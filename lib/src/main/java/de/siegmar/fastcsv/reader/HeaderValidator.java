package de.siegmar.fastcsv.reader;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/// Implementations of this class are used within [NamedCsvRecordHandler] to validate the header
/// of a CSV file once it is determined.
///
/// Example:
/// ```
/// NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder()
///     .headerValidator(HeaderValidator.containsExactly("col1", "col2"))
///     .build();
/// ```
@FunctionalInterface
public interface HeaderValidator {

    /// Builds a validator that throws a [CsvParseException] if the header does not consist of
    /// exactly the given fields, in the given order.
    ///
    /// @param expectedHeader the expected header fields, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `null` is passed or the fields contain `null` elements
    /// @see #containsExactly(List)
    static HeaderValidator containsExactly(final String... expectedHeader) {
        Objects.requireNonNull(expectedHeader, "expectedHeader must not be null");
        return containsExactly(List.of(expectedHeader));
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not consist of
    /// exactly the given fields, in the given order.
    ///
    /// @param expectedHeader the expected header fields, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `null` is passed or the fields contain `null` elements
    /// @see #containsExactly(String...)
    static HeaderValidator containsExactly(final List<String> expectedHeader) {
        Objects.requireNonNull(expectedHeader, "expectedHeader must not be null");
        final List<String> expected = List.copyOf(expectedHeader);
        return header -> {
            if (!expected.equals(header)) {
                throw new CsvParseException("Header mismatch: expected %s but found %s"
                    .formatted(expected, header));
            }
        };
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not contain all
    /// of the given fields, in any order. Additional fields are allowed.
    ///
    /// @param requiredFields the required header fields, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `null` is passed or the fields contain `null` elements
    /// @see #containsAtLeast(List)
    static HeaderValidator containsAtLeast(final String... requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        return containsAtLeast(List.of(requiredFields));
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not contain all
    /// of the given fields, in any order. Additional fields are allowed.
    ///
    /// @param requiredFields the required header fields, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `null` is passed or the fields contain `null` elements
    /// @see #containsAtLeast(String...)
    static HeaderValidator containsAtLeast(final List<String> requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        final List<String> required = List.copyOf(requiredFields);
        return header -> {
            final var missingFields = new LinkedHashSet<>(required);
            header.forEach(missingFields::remove);
            if (!missingFields.isEmpty()) {
                throw new CsvParseException("Header is missing fields %s: %s"
                    .formatted(missingFields, header));
            }
        };
    }

    /// Gets called once the header is determined – either from the first record (that is not a
    /// comment or an empty line) or from a predefined header.
    ///
    /// @param header the header fields, never `null`
    /// @throws RuntimeException if the header is not acceptable
    void validate(List<String> header);

}
