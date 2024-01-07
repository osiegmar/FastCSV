package de.siegmar.fastcsv.reader;

/**
 * A wrapper for a record that contains information necessary for the {@link CsvReader} in order to determine how to
 * process the record.
 *
 * @param <T> the record type
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public final class RecordWrapper<T> {

    private final boolean comment;
    private final boolean emptyLine;
    private final int fieldCount;
    private final T wrappedRecord;

    /**
     * Constructs a new instance.
     * <p>
     * The {@code comment} and {@code emptyLine} parameters are only used if the {@link CsvReader} is configured to
     * skip comments and/or empty lines.
     * The {@code fieldCount} parameter is only used if the {@link CsvReader} is configured to check the number of
     * fields in each record.
     * The {@code wrappedRecord} parameter is the actual record to be returned by the {@link CsvReader}. It can be
     * {@code null} if the record is to be skipped (e.g. if the callback handler already consumed it as a header).
     *
     * @param comment       whether the record denotes a comment (to be skipped if
     *                      {@link CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)} is set to
     *                      {@link CommentStrategy#SKIP}
     * @param emptyLine     whether the record is empty (to be skipped if
     *                      {@link CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)} is set to {@code true})
     * @param fieldCount    the number of fields in the record (to be checked against the number of fields in other
     *                      records if {@link CsvReader.CsvReaderBuilder#ignoreDifferentFieldCount(boolean)} is set to
     *                      {@code false})
     * @param wrappedRecord the actual record to be returned by the {@link CsvReader} or {@code null} to skip the record
     */
    public RecordWrapper(final boolean comment, final boolean emptyLine, final int fieldCount, final T wrappedRecord) {
        this.comment = comment;
        this.emptyLine = emptyLine;
        this.fieldCount = fieldCount;
        this.wrappedRecord = wrappedRecord;
    }

    /**
     * Returns whether the record denotes a comment.
     * <p>
     * This method is only used if the {@link CsvReader} is configured to skip comments.
     *
     * @return {@code true} if the record denotes a comment
     */
    public boolean comment() {
        return comment;
    }

    /**
     * Returns whether the record is empty.
     * <p>
     * This method is only used if the {@link CsvReader} is configured to skip empty lines.
     *
     * @return {@code true} if the record is empty
     */
    public boolean emptyLine() {
        return emptyLine;
    }

    /**
     * Returns the number of fields in the record.
     * <p>
     * This method is only used if the {@link CsvReader} is configured to check the number of fields in each record.
     *
     * @return the number of fields in the record
     */
    public int fieldCount() {
        return fieldCount;
    }

    /**
     * Returns the actual record to be returned by the {@link CsvReader}.
     * <p>
     * This method can return {@code null} if the record is to be skipped (e.g. if the callback handler already consumed
     * it as a header).
     *
     * @return the actual record to be returned by the {@link CsvReader} or {@code null} to skip the record
     */
    public T wrappedRecord() {
        return wrappedRecord;
    }

}
