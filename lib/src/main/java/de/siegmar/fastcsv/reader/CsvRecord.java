package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/// Represents an immutable CSV record with unnamed (indexed) fields.
///
/// The field contents are never `null`. Empty fields are represented as empty strings.
///
/// Instances of this class are immutable and safe for use by multiple threads.
///
/// CSV records are created by [CsvReader] or [IndexedCsvReader].
///
/// @see CsvReader
/// @see IndexedCsvReader
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public class CsvRecord {

    /// The starting line number (starting with 1).
    ///
    /// @see #getStartingLineNumber()
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final long startingLineNumber;

    /// The fields this record is composed of.
    ///
    /// @see #getField(int)
    /// @see #getFields()
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final String[] fields;

    /// Whether this holds a comment.
    ///
    /// @see #isComment()
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final boolean comment;

    CsvRecord(final long startingLineNumber, final String[] fields,
              final boolean comment) {
        this.startingLineNumber = startingLineNumber;
        this.fields = fields;
        this.comment = comment;
    }

    /// Provides the line number at which this record originated, starting from 1.
    ///
    /// This information is particularly valuable for CSV data containing empty lines, comments or records that
    /// span multiple lines, where the record number may deviate from the line number.
    ///
    /// Example:
    /// ```text
    /// 1 foo,bar
    /// 2 foo,"multi
    /// 3 line bar"
    /// 4                    (empty, potentially skipped)
    /// 5 #comment           (potentially skipped)
    /// 6 "latest
    /// 7 record"
    /// ```
    ///
    /// The last record (containing the multi-line field "latest\nrecord") would have a starting line number of 6,
    /// no matter whether empty lines or comments are skipped or not.
    ///
    /// A starting offset of 1 is used to be consistent with the line numbers shown of most text editors.
    ///
    /// Note that this number is only correct if the CSV data was read from the very beginning. If you passed
    /// a [java.io.Reader] to the [CsvReader] and have already read from it, the line number will be
    /// incorrect.
    ///
    /// @return the starting line number of this record, starting from 1
    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    /// Retrieves the field content of the field at the given index, with indexing starting from 0.
    ///
    /// There is always at least one field; an empty line has one empty field.
    ///
    /// For a comment, index 0 returns the comment text (without the comment character).
    ///
    /// @param index index of the field to return
    /// @return the field content, never `null`
    /// @throws IndexOutOfBoundsException if the index is out of range
    public String getField(final int index) {
        return fields[index];
    }

    /// Retrieves all fields of this record as an unmodifiable list.
    ///
    /// The returned list has a minimum size of 1; for an empty line, it contains one empty string.
    ///
    /// @return all fields of this record, never `null`
    public List<String> getFields() {
        // Not using List.of() here for performance reasons, as it copies the array.
        return Collections.unmodifiableList(Arrays.asList(fields));
    }

    /// Gets the count of fields in this record.
    ///
    /// The minimum number of fields is 1; an empty line has one empty field.
    ///
    /// @return the number of fields in this record
    /// @see CsvReader.CsvReaderBuilder#extraFieldStrategy(FieldMismatchStrategy)
    /// @see CsvReader.CsvReaderBuilder#missingFieldStrategy(FieldMismatchStrategy)
    public int getFieldCount() {
        return fields.length;
    }

    /// Indicates whether this holds a comment.
    ///
    /// Retrieve the comment text by calling [#getField(int)] with index 0.
    ///
    /// @return `true` if this holds a comment
    /// @see CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)
    public boolean isComment() {
        return comment;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRecord.class.getSimpleName() + "[", "]")
            .add("startingLineNumber=" + startingLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .toString();
    }

}
