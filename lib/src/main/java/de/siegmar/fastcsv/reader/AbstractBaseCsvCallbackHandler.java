package de.siegmar.fastcsv.reader;

/**
 * Base class for {@link CsvCallbackHandler} implementations that handles their own field storage and record building.
 *
 * @param <T> the type of the record
 */
public abstract class AbstractBaseCsvCallbackHandler<T> implements CsvCallbackHandler<T> {

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
    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    /**
     * Returns whether the current record is a comment.
     *
     * @return {@code true} if the current record is a comment
     */
    public boolean isComment() {
        return comment;
    }

    /**
     * Returns whether the current record is an empty line.
     *
     * @return {@code true} if the current record is an empty line
     */
    public boolean isEmptyLine() {
        return emptyLine;
    }

    /**
     * Returns the number of fields in the current record.
     *
     * @return the number of fields in the current record
     */
    public int getFieldCount() {
        return fieldCount;
    }

    /**
     * {@inheritDoc}
     * Resets the internal state of this handler and delegates to {@link #handleBegin(long)}.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public final void beginRecord(final long startingLineNumber) {
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
    public final void addField(final char[] buf, final int offset, final int len, final boolean quoted) {
        emptyLine = emptyLine && len == 0;
        handleField(fieldCount++, buf, offset, len, quoted);
    }

    /**
     * Handles a field.
     * <p>
     * This method is called for each field in the record.
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
    public final void setComment(final char[] buf, final int offset, final int len) {
        comment = true;
        emptyLine = false;
        handleComment(buf, offset, len);
        fieldCount++;
    }

    /**
     * Handles a comment.
     * <p>
     * This method is called for each comment line.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     */
    protected void handleComment(final char[] buf, final int offset, final int len) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation creates a {@link RecordWrapper} with the current {@link #comment}, {@link #emptyLine} and
     * {@link #fieldCount} and delegates the creation of the actual record to {@link #build()}.
     */
    @Override
    public final RecordWrapper<T> buildRecord() {
        return new RecordWrapper<>(comment, emptyLine, fieldCount, build());
    }

    /**
     * Builds the record.
     *
     * @return the record, or {@code null} if the record is to be skipped
     */
    protected abstract T build();

}
