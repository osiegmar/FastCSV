package de.siegmar.fastcsv.writer;

/// Provides some common [QuoteStrategy] implementations.
public enum QuoteStrategies implements QuoteStrategy {

    /// Quote fields only if quoting is required.
    REQUIRED,

    /// Quote all fields, including those passed as an empty string or `null`.
    ALWAYS {
        @Override
        public boolean quoteNull(final int lineNo, final int fieldIdx) {
            return true;
        }

        @Override
        public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
            return true;
        }

        @Override
        public boolean quoteValue(final int lineNo, final int fieldIdx, final String value) {
            return true;
        }
    },

    /// Quote all fields with non-empty field content.
    NON_EMPTY {
        @Override
        public boolean quoteValue(final int lineNo, final int fieldIdx, final String value) {
            return true;
        }
    },

    /// Quote fields passed as an empty string, but not those passed as `null`, to tell them apart.
    /// This is required for PostgreSQL CSV imports, for example.
    EMPTY {
        @Override
        public boolean quoteEmpty(final int lineNo, final int fieldIdx) {
            return true;
        }
    }

}
