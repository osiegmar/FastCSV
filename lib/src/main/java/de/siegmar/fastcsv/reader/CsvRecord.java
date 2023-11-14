package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Index based CSV-record.
 */
@SuppressWarnings({"PMD.ArrayIsStoredDirectly", "PMD.AvoidFieldNameMatchingMethodName"})
public final class CsvRecord {

    private static final String[] EMPTY = {""};

    private final long originalLineNumber;
    private final String[] fields;
    private final boolean comment;

    CsvRecord(final long originalLineNumber, final boolean comment) {
        this(originalLineNumber, EMPTY, comment);
    }

    CsvRecord(final long originalLineNumber, final String[] fields,
              final boolean comment) {
        this.originalLineNumber = originalLineNumber;
        this.fields = fields;
        this.comment = comment;
    }

    /**
     * Returns the original line number (starting with 1). On multi-line records this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)}.
     *
     * @return the original line number
     */
    public long originalLineNumber() {
        return originalLineNumber;
    }

    /**
     * Gets a field value by its index (starting with 0).
     *
     * @param index index of the field to return
     * @return field value, never {@code null}
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String field(final int index) {
        return fields[index];
    }

    /**
     * Gets all fields of this record as an unmodifiable list.
     *
     * @return all fields of this record, never {@code null}
     */
    public List<String> fields() {
        return Collections.unmodifiableList(Arrays.asList(fields));
    }

    /**
     * Gets the number of fields of this record.
     *
     * @return the number of fields of this record
     * @see CsvReader.CsvReaderBuilder#ignoreDifferentFieldCount(boolean)
     */
    public int fieldCount() {
        return fields.length;
    }

    /**
     * Provides the information if the record is a commented record.
     *
     * @return {@code true} if the record is a commented record
     * @see CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)
     */
    public boolean comment() {
        return comment;
    }

    /**
     * Provides the information if the record is an empty record.
     *
     * @return {@code true} if the record is an empty record
     * @see CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)
     */
    public boolean empty() {
        return fields == EMPTY;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRecord.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .toString();
    }

}
