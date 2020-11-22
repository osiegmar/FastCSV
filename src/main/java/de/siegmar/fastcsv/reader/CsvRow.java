package de.siegmar.fastcsv.reader;

/**
 * Interface for a index based CSV-row.
 */
public interface CsvRow {

    /**
     * Returns the original line number (starting with 1). On multi-line rows this is the starting
     * line number.
     * Empty lines could be skipped via {@link CsvReaderBuilder#skipEmptyRows(boolean)}.
     *
     * @return the original line number
     */
    long getOriginalLineNumber();

    /**
     * Gets a field value by its index (starting with 0).
     *
     * @param index index of the field to return
     * @return field value, never {@code null}
     * @throws IndexOutOfBoundsException if index is out of range
     */
    String getField(int index);

    /**
     * Gets all fields of this row.
     *
     * @return all fields of this row, never {@code null}
     */
    String[] getFields();

    /**
     * Gets the number of fields of this row.
     *
     * @return the number of fields of this row
     */
    int getFieldCount();

}
