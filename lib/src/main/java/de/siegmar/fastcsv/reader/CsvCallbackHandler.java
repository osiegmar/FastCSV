package de.siegmar.fastcsv.reader;

import de.siegmar.fastcsv.util.Nullable;

/// This class defines the methods that are called during the CSV reading process.
///
/// Implementations highly affect the behavior of the [CsvReader]. With great power comes great responsibility.
/// Don't mess up the CSV reading process!
///
/// Even if you need custom handling, you typically don't need to extend this class directly.
/// Check out [AbstractBaseCsvCallbackHandler] first.
///
/// @param <T> the type of the record that is built from the CSV data
@SuppressWarnings("checkstyle:AbstractClassName")
public abstract class CsvCallbackHandler<T> {

    /// Default constructor.
    protected CsvCallbackHandler() {
    }

    /// {@return the type of the record that is built from the CSV data}
    ///
    /// The [CsvReader] will skip
    ///
    /// - comments ([RecordType#COMMENT]) if
    ///   [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)]
    ///   is set to [CommentStrategy#SKIP]
    /// - empty lines ([RecordType#EMPTY]) if
    ///   [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)]
    ///   is set to `true`.
    protected abstract RecordType getRecordType();

    /// {@return the number of fields in the record}
    ///
    /// The [CsvReader] will verify that the number of fields in each record matches the number of fields in the
    /// first record unless
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#extraFieldStrategy(FieldMismatchStrategy)] or
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#missingFieldStrategy(FieldMismatchStrategy)]
    /// are set to [FieldMismatchStrategy#IGNORE] or [FieldMismatchStrategy#SKIP].
    protected abstract int getFieldCount();

    /// Called at the beginning of each record.
    ///
    /// The `startingLineNumber` is the line number where the record starts (starting with 1).
    ///
    /// @param startingLineNumber the line number where the record starts (starting with 1)
    protected abstract void beginRecord(long startingLineNumber);

    /// Called for each field in the record.
    ///
    /// The callbacks are invoked either for a record or for a comment, never both. If this method is called,
    /// it is a record, not a comment.
    ///
    /// The `quoted` parameter indicates whether the field was quoted. It is for informational purposes only.
    /// Each escape pair is already replaced by one quote character, the `offset` points to the first character
    /// after the opening quote character and the `len` does not include the closing quote character.
    /// Hence, a quoted field can be processed in the same way as an unquoted field.
    /// Some implementations need the information whether a field was quoted, e.g., for differentiating between
    /// an unquoted and a quoted empty field (`foo,,bar` vs. `foo,"",bar`) to read the former as `null`.
    ///
    /// The `buf` parameter is the internal buffer that contains the field content (among other data). Do not
    /// attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
    ///
    /// @param buf    the internal buffer that contains the field content (among other data)
    /// @param offset the offset of the field content in the buffer
    /// @param len    the length of the field content
    /// @param quoted `true` if the field was quoted
    protected abstract void addField(char[] buf, int offset, int len, boolean quoted);

    /// Called for each comment.
    ///
    /// Note that the comment character is not part of the comment text.
    ///
    /// This method is not called if
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)]
    /// is set to [CommentStrategy#NONE].
    ///
    /// This method is called at most once between [#beginRecord(long)] and [#buildRecord()].
    /// The callbacks are invoked either for a record or for a comment, never both. If this method is called,
    /// it is a comment, not a record.
    ///
    /// The `buf` parameter is the internal buffer that contains the comment text (among other data). Do not
    /// attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
    ///
    /// @param buf    the internal buffer that contains the comment text (among other data)
    /// @param offset the offset of the comment text in the buffer
    /// @param len    the length of the comment text
    protected abstract void setComment(char[] buf, int offset, int len);

    /// Called for each empty line.
    protected abstract void setEmpty();

    /// Called at the end of each CSV record to build the actual record representation.
    ///
    /// @return the record, or `null` if the callback handler consumes it (e.g., the header) so that it is not returned.
    @Nullable
    protected abstract T buildRecord();

    /// Called at the end of the CSV reading process.
    protected void terminate() {
    }

    /// Returns whether the record just consumed by [#buildRecord()] is the header.
    ///
    /// A consumed header is subject to the field count check, other consumed records are not.
    ///
    /// @return `true` if the consumed record is the header
    boolean isConsumedHeader() {
        return false;
    }

}
