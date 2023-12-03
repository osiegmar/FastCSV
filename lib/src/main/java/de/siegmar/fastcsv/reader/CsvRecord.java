package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents an immutable CSV record with unnamed (indexed) fields.
 * <p>
 * The field values are never {@code null}. Empty fields are represented as empty strings.
 * <p>
 * CSV records are created by {@link CsvReader} or {@link IndexedCsvReader}.
 *
 * @see CsvReader
 * @see IndexedCsvReader
 */
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public class CsvRecord {

    private static final String[] EMPTY = {""};

    /**
     * The starting line number (starting with 1).
     *
     * @see #getStartingLineNumber()
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final long startingLineNumber;

    /**
     * The fields this record is composed of.
     *
     * @see #getField(int)
     * @see #getFields()
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final String[] fields;

    /**
     * If the record is a commented record.
     *
     * @see #isComment()
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    final boolean comment;

    CsvRecord(final long startingLineNumber, final boolean comment) {
        this(startingLineNumber, EMPTY, comment);
    }

    CsvRecord(final long startingLineNumber, final String[] fields,
              final boolean comment) {
        this.startingLineNumber = startingLineNumber;
        this.fields = fields;
        this.comment = comment;
    }

    CsvRecord(final CsvRecord original) {
        startingLineNumber = original.startingLineNumber;
        fields = original.fields;
        comment = original.comment;
    }

    /**
     * Provides the line number at which this record originated, starting from 1.
     * <p>
     * This information is particularly valuable in scenarios involving CSV files containing empty lines as well as
     * multi-line or commented records, where the record number may deviate from the line number.
     * <p>
     * Example:
     * <pre>
     * 1 foo,bar
     * 2 foo,"multi
     * 3 line bar"
     * 4                    (empty, potentially skipped)
     * 5 #commented record  (potentially skipped)
     * 6 "latest
     * 7 record"
     * </pre>
     * The last record (containing the multi-line field "latest\nrecord") would have a starting line number of 6,
     * no matter if empty lines or commented records are skipped or not.
     * <p>
     * A starting offset of 1 is used to be consistent with the line numbers shown of most text editors.
     *
     * @return the starting line number of this record, starting from 1
     */
    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    /**
     * Retrieves the value of a field based on its index, with indexing starting from 0.
     *
     * @param index index of the field to return
     * @return field value, never {@code null}
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String getField(final int index) {
        return fields[index];
    }

    /**
     * Retrieves all fields of this record as an unmodifiable list.
     *
     * @return all fields of this record, never {@code null}
     */
    public List<String> getFields() {
        return Collections.unmodifiableList(Arrays.asList(fields));
    }

    /**
     * Obtains the count of fields in this record.
     *
     * @return the number of fields of this record
     * @see CsvReader.CsvReaderBuilder#ignoreDifferentFieldCount(boolean)
     */
    public int getFieldCount() {
        return fields.length;
    }

    /**
     * Indicates whether the record is a commented record.
     *
     * @return {@code true} if the record is a commented record
     * @see CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)
     */
    public boolean isComment() {
        return comment;
    }

    /**
     * Indicates whether the record is considered empty, signifying that it contains only a single field with an empty
     * string.
     *
     * @return {@code true} if the record is an empty record
     * @see CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)
     */
    public boolean isEmpty() {
        return fields == EMPTY;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRecord.class.getSimpleName() + "[", "]")
            .add("startingLineNumber=" + startingLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .toString();
    }

}
