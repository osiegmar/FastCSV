package de.siegmar.fastcsv.writer;

/// A quote strategy decides whether to quote a field when quoting is not required.
///
/// Quoting is required if the field content contains the field separator, the quote character or a line break,
/// or if the first field of a record begins with the comment character; in that case this strategy is not
/// consulted.
///
/// @see QuoteStrategies
public interface QuoteStrategy {

    /// Determines whether a field passed as `null` is quoted.
    ///
    /// @param lineNo   the 1-based position of the current record among the records and comments written; not a line
    ///                 number, because a record or comment with line breaks counts once
    /// @param fieldIdx the field index (0-based)
    /// @return `true` if a field passed as `null` should be quoted
    default boolean quoteNull(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determines whether a field passed as an empty string is quoted.
    ///
    /// @param lineNo   the 1-based position of the current record among the records and comments written; not a line
    ///                 number, because a record or comment with line breaks counts once
    /// @param fieldIdx the field index (0-based)
    /// @return `true` if a field passed as an empty string should be quoted
    default boolean quoteEmpty(final int lineNo, final int fieldIdx) {
        return false;
    }

    /// Determines whether a field with non-empty field content is quoted (unless quoting is required anyway).
    ///
    /// @param lineNo   the 1-based position of the current record among the records and comments written; not a line
    ///                 number, because a record or comment with line breaks counts once
    /// @param fieldIdx the field index (0-based)
    /// @param value    the field content; never `null` or empty
    /// @return `true` if the field should be quoted
    default boolean quoteValue(final int lineNo, final int fieldIdx, final String value) {
        return false;
    }

}
