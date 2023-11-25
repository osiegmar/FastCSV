package de.siegmar.fastcsv.reader;

import java.util.Locale;

/**
 * Implementations of this class are used to modify fields before they get stored in {@link CsvRecord}.
 * <p>
 * {@snippet :
 * var fields = CsvReader.builder()
 *     .fieldModifier(FieldModifier.TRIM.andThen(FieldModifier.upper(Locale.ENGLISH)))
 *     .build("  foo   ,   bar")
 *     .stream()
 *     .toList();
 *
 * // fields would be: "FOO" and "BAR"
 *}
 *
 * @see de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#fieldModifier(FieldModifier)
 */
public interface FieldModifier {

    /**
     * Modifier that modifies the field value with {@link String#trim()}.
     */
    FieldModifier TRIM = (originalLineNumber, fieldIdx, comment, quoted, field) -> field.trim();

    /**
     * Modifier that modifies the field value with {@link String#strip()}.
     */
    FieldModifier STRIP = (originalLineNumber, fieldIdx, comment, quoted, field) -> field.strip();

    /**
     * Builds modifier that modifies the field value with {@link String#toLowerCase(Locale)}.
     *
     * @param locale use the case transformation rules for this locale
     * @return a new field modifier that converts the input to lower case.
     */
    static FieldModifier lower(final Locale locale) {
        return (originalLineNumber, fieldIdx, comment, quoted, field) -> field.toLowerCase(locale);
    }

    /**
     * Builds modifier that modifies the field value with {@link String#toUpperCase(Locale)}.
     *
     * @param locale use the case transformation rules for this locale
     * @return a new field modifier that converts the input to upper case.
     */
    static FieldModifier upper(final Locale locale) {
        return (originalLineNumber, fieldIdx, comment, quoted, field) -> field.toUpperCase(locale);
    }

    /**
     * Chains multiple modifiers.
     *
     * @param after the next modifier to use.
     * @return a composed field modifier that first applies this modifier and then applies the after modifier
     */
    default FieldModifier andThen(final FieldModifier after) {
        return (originalLineNumber, fieldIdx, comment, quoted, field) ->
            after.modify(originalLineNumber, fieldIdx, comment, quoted,
                modify(originalLineNumber, fieldIdx, comment, quoted, field));
    }

    /**
     * The actual modify method. Gets called for every single field.
     *
     * @param originalLineNumber the original line number (starting with 1)
     * @param fieldIdx           the field index (starting with 0)
     * @param comment            {@code true} if the field is actually a comment record
     * @param quoted             {@code true} if the field was enclosed by the defined quote characters
     * @param field              the field value, never {@code null}
     * @return the modified field value (must not be {@code null})
     */
    String modify(long originalLineNumber, int fieldIdx, boolean comment, boolean quoted, String field);

}
