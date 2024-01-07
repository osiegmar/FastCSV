package de.siegmar.fastcsv.reader;

import java.util.Objects;

/**
 * This interface defines the methods that are called during the CSV reading process.
 * <p>
 * Implementations highly affect the behavior of the {@link CsvReader}. With great power comes great responsibility.
 * Don't mess up the CSV reading process!
 * <p>
 * CsvCallbackHandler implementations are stateful and must not be reused.
 *
 * @param <T> the type of the record that is built from the CSV data
 */
public interface CsvCallbackHandler<T> {

    /**
     * Constructs a callback handler for the given {@link SimpleCsvMapper}.
     *
     * @param <T>    the type of the resulting records
     * @param mapper the mapper
     * @return a callback handler that returns a mapped record for each record
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    static <T> CsvCallbackHandler<T> forSimpleMapper(final SimpleCsvMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return new AbstractCsvCallbackHandler<>() {
            @Override
            protected T buildRecord(final String[] fields) {
                return mapper.build(fields);
            }
        };
    }

    /**
     * Called at the beginning of each record.
     * <p>
     * The {@code startingLineNumber} is the line number where the record starts (starting with 1).
     * See {@link CsvRecord#getStartingLineNumber()}.
     *
     * @param startingLineNumber the line number where the record starts (starting with 1)
     */
    void beginRecord(long startingLineNumber);

    /**
     * Called for each field in the record.
     * <p>
     * A record can either be a comment or a regular record.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     * @param quoted {@code true} if the field was quoted
     */
    void addField(char[] buf, int offset, int len, boolean quoted);

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
     * A record can either be a comment or a regular record.
     *
     * @param buf    the internal buffer that contains the field value (among other data)
     * @param offset the offset of the field value in the buffer
     * @param len    the length of the field value
     */
    void setComment(char[] buf, int offset, int len);

    /**
     * Called at the end of each CSV record in order to build an object representation of the record.
     *
     * @return a wrapper for the record that contains information necessary for the {@link CsvReader} in order to
     *     determine how to process the record. Must not be {@code null}.
     */
    RecordWrapper<T> buildRecord();

    /**
     * Called at the end of the CSV reading process.
     */
    default void terminate() {
    }

}
