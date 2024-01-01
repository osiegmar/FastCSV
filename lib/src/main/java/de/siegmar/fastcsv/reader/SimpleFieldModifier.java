package de.siegmar.fastcsv.reader;

/**
 * A functional interface for modifying CSV fields in a simple way (with reduced functionality).
 * <p>
 * When implementing this interface, comments are ignored (not modified), by default.
 *
 * @see FieldModifiers
 */
@FunctionalInterface
public interface SimpleFieldModifier extends FieldModifier {

    /**
     * Gets called for every single field (that is not a comment).
     *
     * @param field the field value, never {@code null}
     * @return the modified field value (must not be {@code null})
     */
    String modify(String field);

    @Override
    default String modify(final long startingLineNumber, final int fieldIdx, final boolean quoted, final String field) {
        return modify(field);
    }

}
