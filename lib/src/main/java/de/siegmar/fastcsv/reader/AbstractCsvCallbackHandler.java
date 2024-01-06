package de.siegmar.fastcsv.reader;

import de.siegmar.fastcsv.util.Limits;

/**
 * Abstract base class for {@link CsvCallbackHandler} implementations.
 *
 * @param <T> the type of the resulting records
 */
public abstract class AbstractCsvCallbackHandler<T> implements CsvCallbackHandler<T> {

    private static final int INITIAL_FIELDS_SIZE = 32;

    /**
     * The starting line number of the current record.
     * <p>
     * See {@link CsvCallbackHandler#beginRecord(long)} and {@link CsvRecord#getStartingLineNumber()}.
     */
    protected long startingLineNumber;

    /**
     * The internal fields array.
     */
    protected String[] fields;

    /**
     * The total size (sum of all characters) of the current record.
     */
    protected int recordSize;

    /**
     * The current index in the internal fields array.
     */
    protected int idx;

    /**
     * Whether the current record is a comment.
     */
    protected boolean comment;

    /**
     * Constructs a new instance with an initial fields array of size {@value #INITIAL_FIELDS_SIZE}.
     */
    protected AbstractCsvCallbackHandler() {
        this(INITIAL_FIELDS_SIZE);
    }

    AbstractCsvCallbackHandler(final int len) {
        fields = new String[len];
    }

    /**
     * {@inheritDoc}
     * Resets the internal state of this handler.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void beginRecord(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
        idx = 0;
        recordSize = 0;
        comment = false;
    }

    /**
     * {@inheritDoc}
     * Adds the given value to the internal fields array.
     * <p>
     * Extends the array if necessary and keeps track of the total record size and fields count.
     *
     * @throws CsvParseException if the addition exceeds the limit of record size or maximum fields count.
     */
    @Override
    public void addField(final String value, final boolean quoted) {
        recordSize += value.length();
        if (recordSize > Limits.MAX_RECORD_SIZE) {
            throw new CsvParseException(maxRecordSizeExceededMessage(startingLineNumber));
        }

        if (idx == fields.length) {
            extendCapacity();
        }

        fields[idx++] = value;
    }

    private static String maxRecordSizeExceededMessage(final long line) {
        return String.format("Record starting at line %d has surpassed the maximum limit of %d characters",
            line, Limits.MAX_RECORD_SIZE);
    }

    /**
     * {@inheritDoc}
     * Sets the given value as the only field in the internal fields array.
     * <p>
     * Keeps track of the total record size.
     *
     * @throws CsvParseException if the addition exceeds the limit of record size.
     */
    @Override
    public void setComment(final String value) {
        recordSize += value.length();
        if (recordSize > Limits.MAX_RECORD_SIZE) {
            throw new CsvParseException(maxRecordSizeExceededMessage(startingLineNumber));
        }

        if (idx != 0) {
            // Can't happen with the current implementation of CsvParser
            throw new IllegalStateException("Comment must be the first and only field in a record");
        }

        comment = true;
        fields[idx++] = value;
    }

    private void extendCapacity() {
        final int newLen = fields.length * 2;
        if (newLen > Limits.MAX_FIELD_COUNT) {
            throw new CsvParseException("Maximum number of fields exceeded: " + Limits.MAX_FIELD_COUNT);
        }
        final String[] newFields = new String[newLen];
        System.arraycopy(fields, 0, newFields, 0, idx);
        fields = newFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isComment() {
        return comment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmptyLine() {
        return recordSize == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFieldCount() {
        return idx;
    }

    /**
     * {@inheritDoc}
     *
     * This method creates a compact copy of the internal fields array and passes it to {@link #buildRecord(String[])}.
     */
    @Override
    public T buildRecord() {
        final String[] ret = new String[idx];
        System.arraycopy(fields, 0, ret, 0, idx);
        return buildRecord(ret);
    }

    /**
     * Builds a record from the given compact fields array.
     *
     * @param compactFields the compact fields array
     * @return the record
     */
    @SuppressWarnings("PMD.UseVarargs")
    protected abstract T buildRecord(String[] compactFields);

}
