package de.siegmar.fastcsv.reader;

/**
 * This is an enumeration that defines the strategies for handling comments in CSV data that does not conform to RFC.
 */
public enum CommentStrategy {

    /**
     * This strategy does not detect comments. It treats everything as regular fields.
     */
    NONE,

    /**
     * This strategy detects comments but does not return the commented lines.
     */
    SKIP,

    /**
     * This strategy detects and returns the commented lines. The entire line is treated as one field.
     * The comment character itself is removed.
     */
    READ

}
