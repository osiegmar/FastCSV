package de.siegmar.fastcsv.reader;

/**
 * This class defines the methods that are called during the CSV reading process.
 * <p>
 * Implementations highly affect the behavior of the {@link CsvReader}. With great power comes great responsibility.
 * Don't mess up the CSV reading process!
 * <p>
 * Even if you need custom handling, you typically don't need to extend this class directly.
 * Check out {@link AbstractBaseCsvCallbackHandler} first.
 *
 * @param <T> the type of the record that is built from the CSV data
 */
@SuppressWarnings("checkstyle:AbstractClassName")
public abstract class CsvCallbackHandler<T> {

    /**
     * Default constructor.
     */
    protected CsvCallbackHandler() {
    }

    /**
     * Called at the beginning of each record.
     * <p>
     * The {@code startingLineNumber} is the line number where the record starts (starting with 1).
     * See {@link CsvRecord#getStartingLineNumber()}.
     *
     * @param startingLineNumber the line number where the record starts (starting with 1)
     */
    protected abstract void beginRecord(long startingLineNumber);

    /**
     * Called for each field in the record.
     * <p>
     * A record can either be a comment or a regular record. If this method is called, the record is a regular record
     * and cannot be a comment.
     * <p>
     * The {@code quoted} parameter indicates whether the field was quoted. It is for informational purposes only.
     * Any potential escape characters are already removed and the {@code offset} points to the first character
     * after the opening quote and the {@code len} does not include the closing quote. Hence, a quoted field
     * can be processed in the same way as an unquoted field.
     * Some implementations need the information whether a field was quoted, e.g., for differentiating between
     * {@code null} and empty fields ({@code foo,,bar} vs. {@code foo,"",bar}).
     * <p>
     * The {@code buf} parameter is the internal buffer that contains the field value (among other data). Do not
     * attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     * @param quoted {@code true} if the field was quoted
     */
    protected abstract void addField(char[] buf, int offset, int len, boolean quoted);

    /**
     * Called for each comment line.
     * <p>
     * Note that the comment character is not included in the value.
     * <p>
     * This method is not called if
     * {@link de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)}
     * is set to {@link CommentStrategy#NONE}.
     * <p>
     * There can only be one invocation of this method per record.
     * A record can either be a comment or a regular record. If this method is called, the record is a comment
     * and cannot be a regular record.
     * <p>
     * The {@code buf} parameter is the internal buffer that contains the field value (among other data). Do not
     * attempt to modify the buffer or store a reference to it. The buffer is reused for performance reasons.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     */
    protected abstract void setComment(char[] buf, int offset, int len);

    /**
     * Called at the end of each CSV record to build an object representation of the record.
     * <p>
     * The returned wrapper is used by the {@link CsvReader} in order to determine how to process the record.
     *
     * @return the record wrapper or {@code null} if the record should be ignored/skipped
     */
    protected abstract RecordWrapper<T> buildRecord();

    /**
     * Called at the end of the CSV reading process.
     */
    protected void terminate() {
    }

}
