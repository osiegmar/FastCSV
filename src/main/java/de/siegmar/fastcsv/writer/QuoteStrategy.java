package de.siegmar.fastcsv.writer;

/**
 * The strategies that can be used to quote values when writing CSV data.
 */
public enum QuoteStrategy {

    /**
     * Delimits only text fields that requires it. Simple strings (not containing delimiters,
     * field separators, new line or carriage return characters), empty strings and null fields
     * will not be delimited.
     */
    REQUIRED,

    /**
     * In addition to fields that require delimiting also delimit empty text fields to
     * differentiate between empty and null fields.
     * This is required for PostgreSQL CSV imports for example.
     */
    EMPTY,

    /**
     * Delimits any text field regardless of its content (even empty and null fields).
     */
    ALWAYS

}
