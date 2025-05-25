package de.siegmar.fastcsv.reader;

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

    /// {@return whether the record denotes a comment}
    ///
    /// If this method returns `true`, the record is a comment and cannot be a regular record.
    /// The [CsvReader] will skip the record if
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)]
    /// is set to [CommentStrategy#SKIP].
    protected abstract boolean isComment();

    /// {@return whether the line is empty}
    ///
    /// If this method returns `true`, the line is empty.
    /// The [CsvReader] will skip the record if
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)]
    /// is set to `true`.
    protected abstract boolean isEmptyLine();

    /// {@return the number of fields in the record}
    ///
    /// The [CsvReader] will verify that the number of fields in each record matches the number of fields in the
    /// first record unless
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#allowExtraFields(boolean)] or
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#allowMissingFields(boolean)]
    /// are set to `true`.
    protected abstract int getFieldCount();

    /// Called at the beginning of each record.
    ///
    /// The `startingLineNumber` is the line number where the record starts (starting with 1).
    ///
    /// @param startingLineNumber the line number where the record starts (starting with 1)
    protected abstract void beginRecord(long startingLineNumber);

    /// Called for each field in the record.
    ///
    /// A record can either be a comment or a regular record. If this method is called, the record is a regular record
    /// and cannot be a comment.
    ///
    /// The `quoted` parameter indicates whether the field was quoted. It is for informational purposes only.
    /// Any potential escape characters are already removed and the `offset` points to the first character
    /// after the opening quote and the `len` does not include the closing quote. Hence, a quoted field
    /// can be processed in the same way as an unquoted field.
    /// Some implementations need the information whether a field was quoted, e.g., for differentiating between
    /// `null` and empty fields (`foo,,bar` vs. `foo,"",bar`).
    ///
    /// The `buf` parameter is the internal buffer that contains the field value (among other data). Do not
    /// attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
    ///
    /// @param buf    the internal buffer that contains the field value (among other data)
    /// @param offset the offset of the field value in the buffer
    /// @param len    the length of the field value
    /// @param quoted `true` if the field was quoted
    protected abstract void addField(char[] buf, int offset, int len, boolean quoted);

    /// Called for each comment line.
    ///
    /// Note that the comment character is not included in the value.
    ///
    /// This method is not called if
    /// [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)]
    /// is set to [CommentStrategy#NONE].
    ///
    /// There can only be one invocation of this method per record.
    /// A record can either be a comment or a regular record. If this method is called, the record is a comment
    /// and cannot be a regular record.
    ///
    /// The `buf` parameter is the internal buffer that contains the field value (among other data). Do not
    /// attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
    ///
    /// @param buf    the internal buffer that contains the field value (among other data)
    /// @param offset the offset of the field value in the buffer
    /// @param len    the length of the field value
    protected abstract void setComment(char[] buf, int offset, int len);

    /// Called at the end of each CSV record to build the actual record representation.
    ///
    /// @return the record or `null` if the record should be ignored/skipped as it is consumed by the callback handler.
    protected abstract T buildRecord();

    /// Called at the end of the CSV reading process.
    protected void terminate() {
    }

}
