package de.siegmar.fastcsv.writer;

/// A quote strategy decides whether to quote a field when quoting is not required.
///
/// If a field contains characters that require quoting, this strategy is not consulted.
///
/// @see QuoteStrategies
public interface QuoteStrategy {

    /// Determines whether a `null` field is quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @return `true`, if a `null` field should be quoted
    default boolean quoteNull(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determines whether an empty (not `null`) field is quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @return `true`, if an empty field should be quoted
    default boolean quoteEmpty(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determines whether a field with non-empty field content is quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @param value    the field content; never `null`
    /// @return `true`, if the field should be quoted
    default boolean quoteValue(final int lineNo, final int fieldIdx, final String value) {
        return false;
    }

}
