package de.siegmar.fastcsv.reader;

/// Represents the type of record in a CSV file.
public enum RecordType {

    /// A data record containing actual CSV data.
    DATA,

    /// A record that is a comment.
    COMMENT,

    /// An empty line was encountered, without any data or comment.
    EMPTY

}
