package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/// Represents an immutable CSV record with unnamed (indexed) fields.
///
/// The field values are never `null`. Empty fields are represented as empty strings.
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

    /// If the record is a commented record.
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
    /// This information is particularly valuable in scenarios involving CSV files containing empty lines as well as
    /// multi-line or commented records, where the record number may deviate from the line number.
    ///
    /// Example:
    /// ```text
    /// 1 foo,bar
    /// 2 foo,"multi
    /// 3 line bar"
    /// 4                    (empty, potentially skipped)
    /// 5 #commented record  (potentially skipped)
    /// 6 "latest
    /// 7 record"
    /// ```
    ///
    /// The last record (containing the multi-line field "latest\nrecord") would have a starting line number of 6,
    /// no matter if empty lines or commented records are skipped or not.
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

    /// Retrieves the value of a field based on its index, with indexing starting from 0.
    ///
    /// There is always at least one field, even if the line was empty.
    ///
    /// If this records holds a comment, the comment is returned by calling this method with index 0. The comment
    /// character is not included in the returned value.
    ///
    /// @param index index of the field to return
    /// @return field value, never `null`
    /// @throws IndexOutOfBoundsException if the index is out of range
    public String getField(final int index) {
        return fields[index];
    }

    /// Retrieves all fields of this record as an unmodifiable list.
    ///
    /// The returned list has a minimum size of 1, even if the line was empty.
    /// For empty lines, the first field is an empty string.
    ///
    /// @return all fields of this record, never `null`
    public List<String> getFields() {
        return List.of(fields);
    }

    /// Gets the count of fields in this record.
    ///
    /// The minimum number of fields is 1, even if the line was empty.
    ///
    /// @return the number of fields in this record
    /// @see CsvReader.CsvReaderBuilder#allowExtraFields(boolean)
    /// @see CsvReader.CsvReaderBuilder#allowMissingFields(boolean)
    public int getFieldCount() {
        return fields.length;
    }

    /// Indicates whether the record is a commented record.
    ///
    /// Retrieve the comment by calling [#getField(int)] with index 0.
    ///
    /// @return `true` if the record is a commented record
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
