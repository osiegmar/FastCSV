package de.siegmar.fastcsv.reader;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/// Provides some common [FieldModifier] implementations.
///
/// Example usage:
/// ```
/// FieldModifier modifier = FieldModifiers.TRIM
///     .andThen(FieldModifiers.modify(field -> field.toLowerCase(Locale.ENGLISH)));
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
    public static final FieldModifier NOP = new FieldModifier() { };

    /// Modifier that modifies the field value with [String#trim()].
    /// Comments are not modified.
    public static final FieldModifier TRIM = modify(String::trim);

    /// Modifier that modifies the field value with [String#strip()].
    /// Comments are not modified.
    public static final FieldModifier STRIP = modify(String::strip);

    private FieldModifiers() {
        // Utility class
    }

    /// Builds modifier that modifies the field value with [String#toLowerCase(Locale)].
    /// Comments are not modified.
    ///
    /// @param locale use the case transformation rules for this locale
    /// @return a new field modifier that converts the input to lower-case.
    /// @deprecated Use [#modify(Function)] instead.
    @Deprecated(since = "3.7.0", forRemoval = true)
    public static FieldModifier lower(final Locale locale) {
        return modify(field -> field.toLowerCase(locale));
    }

    /// Builds modifier that modifies the field value with [String#toUpperCase(Locale)].
    /// Comments are not modified.
    ///
    /// @param locale use the case transformation rules for this locale
    /// @return a new field modifier that converts the input to upper-case.
    /// @deprecated Use [#modify(Function)] instead.
    @Deprecated(since = "3.7.0", forRemoval = true)
    public static FieldModifier upper(final Locale locale) {
        return modify(field -> field.toUpperCase(locale));
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
