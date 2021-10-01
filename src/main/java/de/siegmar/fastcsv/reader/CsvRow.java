package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Index based CSV-row.
 */
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public final class CsvRow {

    private static final String[] EMPTY = {""};

    private final long originalLineNumber;
    private final long startingOffset;
    private final String[] fields;
    private final boolean comment;

    CsvRow(final long originalLineNumber, final long startingOffset, final boolean comment) {
        this(originalLineNumber, startingOffset, EMPTY, comment);
    }

    CsvRow(final long originalLineNumber, final long startingOffset, final String[] fields,
           final boolean comment) {
        this.originalLineNumber = originalLineNumber;
        this.startingOffset = startingOffset;
        this.fields = fields;
        this.comment = comment;
    }

    /**
     * Returns the original line number (starting with 1). On multi-line rows this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReader.CsvReaderBuilder#skipEmptyRows(boolean)}.
     * <p>
     * When calling {@link CsvReader#resetBuffer()} this value starts from 1 again.
     *
     * @return the original line number
     */
    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    /**
     * Returns the starting offset (data position in the underlying reader) of this row.
     * <p>
     * When calling {@link CsvReader#resetBuffer()} this value starts from 0 again.
     *
     * @return this row starting offset.
     */
    public long getStartingOffset() {
        return startingOffset;
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
     * Gets all fields of this row as an unmodifiable list.
     *
     * @return all fields of this row, never {@code null}
     */
    public List<String> getFields() {
        return Collections.unmodifiableList(Arrays.asList(fields));
    }

    /**
     * Gets the number of fields of this row.
     *
     * @return the number of fields of this row
     * @see CsvReader.CsvReaderBuilder#errorOnDifferentFieldCount(boolean)
     */
    public int getFieldCount() {
        return fields.length;
    }

    /**
     * Provides the information if the row is a commented row.
     *
     * @return {@code true} if the row is a commented row
     * @see CsvReader.CsvReaderBuilder#commentStrategy(CommentStrategy)
     */
    public boolean isComment() {
        return comment;
    }

    /**
     * Provides the information if the row is an empty row.
     *
     * @return {@code true} if the row is an empty row
     * @see CsvReader.CsvReaderBuilder#skipEmptyRows(boolean)
     */
    public boolean isEmpty() {
        return fields == EMPTY;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRow.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("startingOffset=" + startingOffset)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .toString();
    }

}
