package de.siegmar.fastcsv.reader;

import java.util.Objects;

/**
 * Base class for {@link CsvCallbackHandler} implementations that handles their own field storage and record building.
 * <p>
 * This implementation is stateful and must not be reused.
 *
 * @param <T> the type of the record
 */
public abstract class AbstractBaseCsvCallbackHandler<T> extends CsvCallbackHandler<T> {

    private long startingLineNumber;
    private boolean comment;
    private boolean emptyLine;
    private int fieldCount;

    /**
     * Constructs a new instance.
     */
    protected AbstractBaseCsvCallbackHandler() {
    }

    /**
     * The starting line number of the current record.
     * <p>
     * See {@link CsvCallbackHandler#beginRecord(long)} and {@link CsvRecord#getStartingLineNumber()}.
     *
     * @return the starting line number of the current record
     */
    protected long getStartingLineNumber() {
        return startingLineNumber;
    }

    /**
     * Returns whether the current record is a comment.
     *
     * @return {@code true} if the current record is a comment
     */
    protected boolean isComment() {
        return comment;
    }

    /**
     * Returns whether the current record is an empty line.
     *
     * @return {@code true} if the current record is an empty line
     */
    protected boolean isEmptyLine() {
        return emptyLine;
    }

    /**
     * Returns the number of fields in the current record.
     *
     * @return the number of fields in the current record
     */
    protected int getFieldCount() {
        return fieldCount;
    }

    /**
     * {@inheritDoc}
     * Resets the internal state of this handler and delegates to {@link #handleBegin(long)}.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    protected final void beginRecord(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
        fieldCount = 0;
        comment = false;
        emptyLine = true;
        handleBegin(startingLineNumber);
    }

    /**
     * Handles the beginning of a record.
     * <p>
     * This method is called at the beginning of each record.
     *
     * @param startingLineNumber the line number where the record starts (starting with 1)
     */
    @SuppressWarnings("checkstyle:HiddenField")
    protected void handleBegin(final long startingLineNumber) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to {@link #handleField(int, char[], int, int, boolean)} after updating the
     * {@link #emptyLine} flag and before incrementing the {@link #fieldCount}.
     */
    @Override
    protected final void addField(final char[] buf, final int offset, final int len, final boolean quoted) {
        emptyLine = emptyLine && len == 0;
        handleField(fieldCount++, buf, offset, len, quoted);
    }

    /**
     * Handles a field.
     * <p>
     * This method is called for each field in the record.
     * <p>
     * See {@link CsvCallbackHandler#addField(char[], int, int, boolean)} for more details on the parameters.
     *
     * @param fieldIdx the index of the field in the record (starting with 0)
     * @param buf      the internal buffer that contains the field value (among other data)
     * @param offset   the offset of the field value in the buffer
     * @param len      the length of the field value
     * @param quoted   {@code true} if the field was quoted
     */
    protected void handleField(final int fieldIdx, final char[] buf, final int offset, final int len,
                               final boolean quoted) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation delegates to {@link #handleComment(char[], int, int)} after updating the {@link #comment}
     * and {@link #emptyLine} flag and before incrementing the {@link #fieldCount}.
     */
    @Override
    protected final void setComment(final char[] buf, final int offset, final int len) {
        comment = true;
        emptyLine = false;
        handleComment(buf, offset, len);
        fieldCount++;
    }

    /**
     * Handles a comment.
     * <p>
     * This method is called for each comment line.
     * <p>
     * See {@link CsvCallbackHandler#setComment(char[], int, int)} for more details on the parameters.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     */
    protected void handleComment(final char[] buf, final int offset, final int len) {
    }

    /**
     * Builds a wrapper for the record that contains information necessary for the {@link CsvReader} in order to
     * determine how to process the record.
     *
     * @param record the actual record to be returned by the {@link CsvReader}, must not be {@code null}
     * @return the wrapper for the actual record
     * @throws NullPointerException if {@code null} is passed for {@code record}
     */
    protected RecordWrapper<T> wrapRecord(final T record) {
        return new RecordWrapper<>(comment, emptyLine, fieldCount,
            Objects.requireNonNull(record, "record must not be null"));
    }

}
