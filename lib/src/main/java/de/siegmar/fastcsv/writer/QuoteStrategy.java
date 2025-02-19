package de.siegmar.fastcsv.writer;

/// A quote strategy is used to decide whether to quote fields if quoting is optional (as per RFC 4180).
///
/// If a field contains characters for which the RFC dictates quoting,
/// this QuoteStrategy won't be called for a decision.
///
/// @see QuoteStrategies
public interface QuoteStrategy {

    /// Determine if a `null` field should be quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @return `true`, if a `null` field should be quoted
    default boolean quoteNull(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determine if an empty (not `null`) field should be quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @return `true`, if an empty field should be quoted
    default boolean quoteEmpty(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determine if a data containing field should be quoted.
    ///
    /// @param lineNo   the line number (1-based)
    /// @param fieldIdx the field index (0-based)
    /// @param value    the field value
    /// @return `true`, if a data containing field should be quoted
    default boolean quoteNonEmpty(final int lineNo, final int fieldIdx, final String value) {
        return false;
    }

}
