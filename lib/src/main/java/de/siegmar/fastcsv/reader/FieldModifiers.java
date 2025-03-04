package de.siegmar.fastcsv.reader;

import java.util.Locale;

/// Provides some common [FieldModifier] implementations.
///
/// Example usage:
/// ```
/// FieldModifier modifier = FieldModifiers.TRIM.andThen(FieldModifiers.upper(Locale.ENGLISH));
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
    public static final FieldModifier NOP = (SimpleFieldModifier) field -> field;

    /// Modifier that modifies the field value with [String#trim()].
    /// Comments are not modified.
    public static final FieldModifier TRIM = (SimpleFieldModifier) String::trim;

    /// Modifier that modifies the field value with [String#strip()].
    /// Comments are not modified.
    public static final FieldModifier STRIP = (SimpleFieldModifier) String::strip;

    private FieldModifiers() {
        // Utility class
    }

    /// Builds modifier that modifies the field value with [String#toLowerCase(Locale)].
    /// Comments are not modified.
    ///
    /// @param locale use the case transformation rules for this locale
    /// @return a new field modifier that converts the input to lower-case.
    public static FieldModifier lower(final Locale locale) {
        return (SimpleFieldModifier) field -> field.toLowerCase(locale);
    }

    /// Builds modifier that modifies the field value with [String#toUpperCase(Locale)].
    /// Comments are not modified.
    ///
    /// @param locale use the case transformation rules for this locale
    /// @return a new field modifier that converts the input to upper-case.
    public static FieldModifier upper(final Locale locale) {
        return (SimpleFieldModifier) field -> field.toUpperCase(locale);
    }

}
