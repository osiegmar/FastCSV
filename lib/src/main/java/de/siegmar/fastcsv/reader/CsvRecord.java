package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Index based CSV-record.
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
     * Returns the starting line number (starting with 1). On multi-line records this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReader.CsvReaderBuilder#skipEmptyLines(boolean)}.
     *
     * @return the starting line number
     */
    public long getStartingLineNumber() {
        return startingLineNumber;
    }

    /**
     * Gets a field value by its index (starting with 0).
     *
     * @param index index of the field to return
     * @return field value, never {@code null}
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String getField(final int index) {
        return fields[index];
    }

    /**
     * Gets all fields of this record as an unmodifiable list.
     *
     * @return all fields of this record, never {@code null}
     */
    public List<String> getFields() {
        return Collections.unmodifiableList(Arrays.asList(fields));
    }

    /**
     * Gets the number of fields of this record.
     *
     * @return the number of fields of this record
     * @see CsvReader.CsvReaderBuilder#ignoreDifferentFieldCount(boolean)
     */
    public int getFieldCount() {
        return fields.length;
    }

    /**
     * Provides the information if the record is a commented record.
     *
     * @return {@code true} if the record is a commented record
     * @see CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)
     */
    public boolean isComment() {
        return comment;
    }

    /**
     * Provides the information if the record is an empty record.
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
