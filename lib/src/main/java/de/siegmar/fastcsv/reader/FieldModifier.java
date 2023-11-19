package de.siegmar.fastcsv.reader;

import java.util.Locale;

/**
 * FOO.
 */
public interface FieldModifier {

    /**
     * Modifier that modifies the field value with {@link String#trim()}.
     */
    FieldModifier TRIM = (originalLineNumber, fieldNo, comment, field) -> field.trim();

    /**
     * Modifier that modifies the field value with {@link String#strip()}.
     */
    FieldModifier STRIP = (originalLineNumber, fieldNo, comment, field) -> field.strip();

    /**
     * Builds modifier that modifies the field value with {@link String#toLowerCase(Locale)}.
     * @param locale use the case transformation rules for this locale
     * @return asdf
     */
    static FieldModifier lower(final Locale locale) {
        return (originalLineNumber, fieldNo, comment, field) -> field.toLowerCase(locale);
    }

    /**
     * Builds modifier that modifies the field value with {@link String#toUpperCase(Locale)}.
     * @param locale use the case transformation rules for this locale
     * @return asdf
     */
    static FieldModifier upper(final Locale locale) {
        return (originalLineNumber, fieldNo, comment, field) -> field.toUpperCase(locale);
    }

    /**
     * Chains multiple modifiers.
     *
     * @param after the next modifier to use.
     * @return this new modifier ????.
     */
    default FieldModifier andThen(final FieldModifier after) {
        return (originalLineNumber, fieldNo, comment, field) ->
            after.modify(originalLineNumber, fieldNo, comment,
                modify(originalLineNumber, fieldNo, comment, field));
    }

    /**
     * The actual modify method. Gets called for every single field.
     *
     * @param originalLineNumber asdf
     * @param fieldNo asdf
     * @param comment asdf
     * @param field asdf
     * @return the modified field value
     */
    String modify(long originalLineNumber, int fieldNo, boolean comment, String field);

}
