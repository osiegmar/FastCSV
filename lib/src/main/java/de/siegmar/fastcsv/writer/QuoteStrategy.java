package de.siegmar.fastcsv.writer;

/**
 * The strategies that can be used to quote values when writing CSV data.
 */
public enum QuoteStrategy {

    /**
     * Enclose fields only with quotes if required. That is, if the field contains:
     * <ul>
     *     <li>field separator</li>
     *     <li>quote character</li>
     *     <li>comment character</li>
     *     <li>newline character(s) (CR / LF / CRLF)</li>
     * </ul>
     *
     * Empty strings and {@code null} fields will not be enclosed with quotes.
     */
    REQUIRED,

    /**
     * In addition to fields that require quote enclosing also delimit empty text fields to
     * differentiate between empty and {@code null} fields.
     * This is required for PostgreSQL CSV imports for example.
     */
    EMPTY,

    /**
     * Enclose any text field with quotes regardless of its content (even empty and {@code null} fields).
     */
    ALWAYS,

    /**
     * Enclose any text field with quotes if it has content, excluding empty and {@code null} fields.
     */
    NON_EMPTY
}
