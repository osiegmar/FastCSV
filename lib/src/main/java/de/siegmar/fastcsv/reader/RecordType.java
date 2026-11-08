package de.siegmar.fastcsv.reader;

/// What a callback handler has received: a record, an empty line or a comment.
public enum RecordType {

    /// A record that is not an empty line.
    DATA,

    /// A comment.
    COMMENT,

    /// An empty line, read as a record with one empty field.
    EMPTY

}
