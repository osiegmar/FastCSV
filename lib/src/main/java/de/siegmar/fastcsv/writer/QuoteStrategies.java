package de.siegmar.fastcsv.writer;

/**
 * Provides some common {@link QuoteStrategy} implementations.
 */
public final class QuoteStrategies {

    /**
     * Enclose any field with quotes regardless of its content (even empty and {@code null} fields).
     */
    public static final QuoteStrategy ALWAYS = new QuoteStrategy() {
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
    public static final QuoteStrategy NON_EMPTY = new QuoteStrategy() {
        @Override
        public boolean quoteNonEmpty(final int lineNo, final int fieldIdx, final String value) {
            return true;
        }
    };

    /**
     * Enclose empty but not @{code null} fields in order to differentiate them.
     * This is required for PostgreSQL CSV imports for example.
     */
    public static final QuoteStrategy EMPTY = new QuoteStrategy() {
        @Override
        public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
            return true;
        }
    };

    private QuoteStrategies() {
        // utility class
    }

}
