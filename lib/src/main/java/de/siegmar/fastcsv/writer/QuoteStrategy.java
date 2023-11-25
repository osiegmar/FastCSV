package de.siegmar.fastcsv.writer;

/**
 * A quote strategy is used to decide whether to quote fields if quoting is optional (as per RFC 4180).
 * <p>
 * If a field contains characters for which the RFC dictates quoting, this QuoteStrategy won't be called for a decision.
 */
public interface QuoteStrategy {

    /**
     * Enclose any field with quotes regardless of its content (even empty and {@code null} fields).
     */
    QuoteStrategy ALWAYS = new QuoteStrategy() {
        @Override
        public boolean quoteNull(final int lineNo, final int fieldIdx) {
            return true;
        }

        @Override
        public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
            return true;
        }

        @Override
        public boolean quoteNonEmpty(final int lineNo, final int fieldIdx, final String value) {
            return true;
        }
    };

    /**
     * Enclose any field with quotes if it has content (is not empty or {@code null}).
     */
    QuoteStrategy NON_EMPTY = new QuoteStrategy() {
        @Override
        public boolean quoteNonEmpty(final int lineNo, final int fieldIdx, final String value) {
            return true;
        }
    };

    /**
     * Enclose empty but not @{code null} fields in order to differentiate them.
     * This is required for PostgreSQL CSV imports for example.
     */
    QuoteStrategy EMPTY = new QuoteStrategy() {
        @Override
        public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
            return true;
        }
    };

    /**
     * Determine if a {@code null} field should be quoted.
     *
     * @param lineNo   the line number (1-based)
     * @param fieldIdx the field index (0-based)
     * @return {@code true}, if a {@code null} field should be quoted
     */
    default boolean quoteNull(final int lineNo, int fieldIdx) {
        return false;
    }

    /**
     * Determine if an empty (not {@code null}) field should be quoted.
     *
     * @param lineNo   the line number (1-based)
     * @param fieldIdx the field index (0-based)
     * @return {@code true}, if an empty field should be quoted
     */
    default boolean quoteEmpty(final int lineNo, int fieldIdx) {
        return false;
    }

    /**
     * Determine if a data containing field should be quoted.
     *
     * @param lineNo   the line number (1-based)
     * @param fieldIdx the field index (0-based)
     * @param value    the field value
     * @return {@code true}, if a data containing field should be quoted
     */
    default boolean quoteNonEmpty(final int lineNo, int fieldIdx, String value) {
        return false;
    }

}
