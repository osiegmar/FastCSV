package de.siegmar.fastcsv.reader;

/// Base class for [CsvCallbackHandler] implementations that handles their own field storage and record building.
///
/// This implementation is stateful and must not be reused.
///
/// @param <T> the type of the record
public abstract class AbstractBaseCsvCallbackHandler<T> extends CsvCallbackHandler<T> {

    private long startingLineNumber;
    private RecordType recordType = RecordType.DATA;
    private int fieldCount;

    /// Constructs a new instance.
    protected AbstractBaseCsvCallbackHandler() {
    }

    /// The starting line number of the current record.
    ///
    /// See [#beginRecord(long)] and [#getStartingLineNumber()].
    ///
    /// @return the starting line number of the current record
    protected long getStartingLineNumber() {
        return startingLineNumber;
    }

    /// {@inheritDoc}
    @Override
    protected RecordType getRecordType() {
        return recordType;
    }

    /// {@return the number of fields in the current record.}
    @Override
    protected int getFieldCount() {
        return fieldCount;
    }

    /// {@inheritDoc}
    /// Resets the internal state of this handler and delegates to [#handleBegin(long)].
    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    protected final void beginRecord(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
        fieldCount = 0;
        recordType = RecordType.DATA;
        handleBegin(startingLineNumber);
    }

    /// Handles the beginning of a record.
    ///
    /// This method is called at the beginning of each record.
    ///
    /// @param startingLineNumber the line number where the record starts (starting with 1)
    @SuppressWarnings("checkstyle:HiddenField")
    protected void handleBegin(final long startingLineNumber) {
    }

    /// {@inheritDoc}
    ///
    /// This implementation delegates to [#handleField(int, char\[\], int, int, boolean)]
    /// before incrementing the [#fieldCount].
    @Override
    protected final void addField(final char[] buf, final int offset, final int len, final boolean quoted) {
        handleField(fieldCount++, buf, offset, len, quoted);
    }

    /// Handles a field.
    ///
    /// This method is called for each field in the record.
    ///
    /// See [#addField(char\[\],int,int,boolean)] for more details on the parameters.
    ///
    /// @param fieldIdx the index of the field in the record (starting with 0)
    /// @param buf      the internal buffer that contains the field value (among other data)
    /// @param offset   the offset of the field value in the buffer
    /// @param len      the length of the field value
    /// @param quoted   `true` if the field was quoted
    protected void handleField(final int fieldIdx, final char[] buf, final int offset, final int len,
                               final boolean quoted) {
    }

    /// {@inheritDoc}
    ///
    /// This implementation delegates to [#handleComment(char\[\],int,int)] after updating the [#recordType]
    /// and before incrementing the [#fieldCount].
    @Override
    protected final void setComment(final char[] buf, final int offset, final int len) {
        recordType = RecordType.COMMENT;
        handleComment(buf, offset, len);
        fieldCount = 1;
    }

    /// Handles a comment.
    ///
    /// This method is called for each comment line.
    ///
    /// See [#setComment(char\[\],int,int)] for more details on the parameters.
    ///
    /// @param buf    the internal buffer that contains the field value (among other data)
    /// @param offset the offset of the field value in the buffer
    /// @param len    the length of the field value
    protected void handleComment(final char[] buf, final int offset, final int len) {
    }

    /// {@inheritDoc}
    ///
    /// This implementation delegates to [#handleEmpty()] after updating the [#recordType]
    /// and before setting the [#fieldCount] to 1.
    @Override
    protected final void setEmpty() {
        recordType = RecordType.EMPTY;
        handleEmpty();
        fieldCount = 1;
    }

    /// Handles an empty line.
    ///
    /// This method is called for each empty line.
    protected void handleEmpty() {
    }

}
