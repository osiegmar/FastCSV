package de.siegmar.fastcsv.reader;

/**
 * Implementations of this class are used to modify fields before they are passed to the {@link CsvCallbackHandler}
 * and finally stored in a record.
 *
 * @see FieldModifiers
 * @see SimpleFieldModifier
 * @see CsvReader.CsvReaderBuilder#fieldModifier(FieldModifier)
 */
public interface FieldModifier {

    /**
     * Gets called for every single field (that is not a comment).
     * Default implementation returns the field as is.
     *
     * @param startingLineNumber the starting line number (starting with 1)
     * @param fieldIdx           the field index (starting with 0)
     * @param quoted             {@code true} if the field was enclosed by the defined quote characters
     * @param field              the field value, never {@code null}
     * @return the modified field value (must not be {@code null})
     */
    default String modify(final long startingLineNumber, final int fieldIdx, final boolean quoted, final String field) {
        return field;
    }

    /**
     * Gets called for every comment.
     * Default implementation returns the field as is.
     *
     * @param startingLineNumber the starting line number (starting with 1)
     * @param field              the field value (comment), never {@code null}
     * @return the modified field value (must not be {@code null})
     */
    default String modifyComment(final long startingLineNumber, final String field) {
        return field;
    }

    /**
     * Chains multiple modifiers.
     *
     * @param after the next modifier to use.
     * @return a composed field modifier that first applies this modifier and then applies the after modifier
     */
    default FieldModifier andThen(final FieldModifier after) {
        return new FieldModifier() {
            @Override
            public String modify(final long startingLineNumber, final int fieldIdx, final boolean quoted,
                                 final String field) {
                return after.modify(startingLineNumber, fieldIdx, quoted,
                    FieldModifier.this.modify(startingLineNumber, fieldIdx, quoted, field));
            }

            @Override
            public String modifyComment(final long startingLineNumber, final String field) {
                return after.modifyComment(startingLineNumber,
                    FieldModifier.this.modifyComment(startingLineNumber, field));
            }
        };
    }

}
