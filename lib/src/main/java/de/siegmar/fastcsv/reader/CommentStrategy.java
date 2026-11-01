package de.siegmar.fastcsv.reader;

/// Strategies for handling comments.
///
/// Comments are not part of RFC 4180.
/// Where a record would begin with the comment character, a comment is read instead.
public enum CommentStrategy {

    /// Comments are not recognized; a line that begins with the comment character is read as a record.
    NONE,

    /// Comments are recognized and skipped.
    SKIP,

    /// Comments are recognized and returned; the comment text (without the comment character) is the only field.
    READ

}
