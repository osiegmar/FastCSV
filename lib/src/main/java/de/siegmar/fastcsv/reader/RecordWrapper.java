package de.siegmar.fastcsv.reader;

/// A wrapper for a record that contains information necessary for the [CsvReader] in order to determine how to
/// process the record.
///
/// @param <T> the record type
@SuppressWarnings("checkstyle:VisibilityModifier")
public final class RecordWrapper<T> {

    private final boolean comment;
    private final boolean emptyLine;
    private final int fieldCount;
    private final T wrappedRecord;

    /// Constructs a new instance.
    ///
    /// The `comment` and `emptyLine` parameters are only used if the [CsvReader] is configured to
    /// skip comments and/or empty lines.
    /// The `fieldCount` parameter is only used if the [CsvReader] is configured to check the number of
    /// fields in each record.
    /// The `wrappedRecord` parameter is the actual record to be returned by the [CsvReader].
    ///
    /// @param comment       whether the record denotes a comment (to be skipped if
    ///                      [#commentStrategy(CommentStrategy)] is set to
    ///                      [CommentStrategy#SKIP]
    /// @param emptyLine     whether the record is empty (to be skipped if
    ///                      [#skipEmptyLines(boolean)] is set to `true`)
    /// @param fieldCount    the number of fields in the record (to be checked against the number of fields in other
    ///                      records if [#ignoreDifferentFieldCount(boolean)] is set to
    ///                      `false`)
    /// @param wrappedRecord the actual record to be returned by the [CsvReader], must not be `null`
    RecordWrapper(final boolean comment, final boolean emptyLine, final int fieldCount, final T wrappedRecord) {
        this.comment = comment;
        this.emptyLine = emptyLine;
        this.fieldCount = fieldCount;
        this.wrappedRecord = wrappedRecord;
    }

    /// Returns whether the record denotes a comment.
    ///
    /// This method is only used if the [CsvReader] is configured to skip comments.
    ///
    /// @return `true` if the record denotes a comment
    public boolean isComment() {
        return comment;
    }

    /// Returns whether the record is empty.
    ///
    /// This method is only used if the [CsvReader] is configured to skip empty lines.
    ///
    /// @return `true` if the record is empty
    public boolean isEmptyLine() {
        return emptyLine;
    }

    /// Returns the number of fields in the record.
    ///
    /// This method is only used if the [CsvReader] is configured to check the number of fields in each record.
    ///
    /// @return the number of fields in the record
    public int getFieldCount() {
        return fieldCount;
    }

    /// Returns the actual record to be returned by the [CsvReader].
    ///
    /// @return the actual record to be returned by the [CsvReader], never `null`
    public T getWrappedRecord() {
        return wrappedRecord;
    }

}
