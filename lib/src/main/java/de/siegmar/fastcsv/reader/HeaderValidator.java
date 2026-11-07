package de.siegmar.fastcsv.reader;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import de.siegmar.fastcsv.util.Util;

/// Implementations of this class are used within [NamedCsvRecordHandler] to validate the header
/// of CSV data once it is determined.
///
/// Example:
/// ```
/// NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder()
///     .headerValidator(HeaderValidator.containsExactly("id", "name"))
///     .build();
/// ```
@FunctionalInterface
public interface HeaderValidator {

    /// Builds a validator that throws a [CsvParseException] if the header does not consist of
    /// exactly the given field names, in the given order.
    ///
    /// @param expectedHeader the expected field names, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `expectedHeader` is `null` or contains a `null` element
    /// @see #containsExactly(List)
    static HeaderValidator containsExactly(final String... expectedHeader) {
        Objects.requireNonNull(expectedHeader, "expectedHeader must not be null");
        return containsExactly(List.of(expectedHeader));
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not consist of
    /// exactly the given field names, in the given order.
    ///
    /// @param expectedHeader the expected field names, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `expectedHeader` is `null` or contains a `null` element
    /// @see #containsExactly(String...)
    static HeaderValidator containsExactly(final List<String> expectedHeader) {
        Objects.requireNonNull(expectedHeader, "expectedHeader must not be null");
        final List<String> expected = List.copyOf(expectedHeader);
        return header -> {
            if (!expected.equals(header)) {
                throw new CsvParseException(Util.escapeControlCharacters(
                    "Header mismatch: expected %s but found %s".formatted(expected, header)));
            }
        };
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not contain all
    /// of the given field names, in any order. Additional field names are allowed.
    ///
    /// @param requiredFields the required field names, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `requiredFields` is `null` or contains a `null` element
    /// @see #containsAtLeast(List)
    static HeaderValidator containsAtLeast(final String... requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        return containsAtLeast(List.of(requiredFields));
    }

    /// Builds a validator that throws a [CsvParseException] if the header does not contain all
    /// of the given field names, in any order. Additional field names are allowed.
    ///
    /// @param requiredFields the required field names, must not be `null`
    /// @return the validator
    /// @throws NullPointerException if `requiredFields` is `null` or contains a `null` element
    /// @see #containsAtLeast(String...)
    static HeaderValidator containsAtLeast(final List<String> requiredFields) {
        Objects.requireNonNull(requiredFields, "requiredFields must not be null");
        final List<String> required = List.copyOf(requiredFields);
        return header -> {
            final var missingFields = new LinkedHashSet<>(required);
            header.forEach(missingFields::remove);
            if (!missingFields.isEmpty()) {
                throw new CsvParseException(Util.escapeControlCharacters(
                    "Header is missing fields %s: %s".formatted(missingFields, header)));
            }
        };
    }

    /// Gets called once the header is determined – either from the first record (empty lines and
    /// comments never count as the first record) or from an external header.
    ///
    /// @param header the field names of the header, never `null`
    /// @throws RuntimeException if the header is not acceptable
    void validate(List<String> header);

}
