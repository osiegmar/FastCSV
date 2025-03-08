package de.siegmar.fastcsv.reader;

import java.util.Objects;
import java.util.function.Function;

/// Provides some common [FieldModifier] implementations.
///
/// Example usage:
/// ```
/// FieldModifier modifier = FieldModifiers.TRIM
///     .andThen(FieldModifiers.modify(field -> field.toUpperCase(Locale.ENGLISH)));
/// CsvRecordHandler handler = CsvRecordHandler.of(c -> c.fieldModifier(modifier));
/// List<CsvRecord> records = CsvReader.builder()
///     .build(handler, "  foo   ,   bar")
///     .stream()
///     .collect(Collectors.toList());
///
/// // fields would be: "FOO" and "BAR"
/// ```
public final class FieldModifiers {

    /// Modifier that does not modify anything.
    public static final FieldModifier NOP = modify(field -> field);

    /// Modifier that modifies the field value with [String#trim()].
    /// Comments are not modified.
    public static final FieldModifier TRIM = modify(String::trim);

    /// Modifier that modifies the field value with [String#strip()].
    /// Comments are not modified.
    public static final FieldModifier STRIP = modify(String::strip);

    private FieldModifiers() {
        // Utility class
    }

    /// Builds a modifier that modifies the field value using the provided function.
    /// Comments are not modified.
    ///
    /// @param function the function to modify the field value. The contract of
    ///                 [FieldModifier#modify(long, int, boolean, String)] applies:
    ///                 the value passed to the function is never `null` and the return value must not be `null`.
    /// @return a new field modifier that applies the function to the field value
    /// @throws NullPointerException if the function is `null`
    public static FieldModifier modify(final Function<? super String, String> function) {
        Objects.requireNonNull(function, "function must not be null");
        return new FieldModifier() {
            @Override
            public String modify(final long startingLineNumber, final int fieldIdx,
                                 final boolean quoted, final String field) {
                return function.apply(field);
            }
        };
    }

}
