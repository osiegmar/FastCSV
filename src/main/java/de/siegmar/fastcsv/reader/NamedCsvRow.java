package de.siegmar.fastcsv.reader;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for a header name based CSV-row.
 */
public interface NamedCsvRow extends CsvRow {

    /**
     * Gets a field value by its name.
     *
     * @param name field name
     * @return field value, never {@code null}
     * @throws java.util.NoSuchElementException if this row has no such field
     * @see #findField(String)
     */
    String getField(String name);

    /**
     * Finds a field value by its name.
     *
     * @param name field name
     * @return field value, {@link Optional#empty()} if this row has no such field
     */
    Optional<String> findField(String name);

    /**
     * Gets an unmodifiable map of header names and field values of this row.
     * <p>
     * The map will always contain all header names - even if their value is {@code null}.
     *
     * @return an unmodifiable map of header names and field values of this row
     */
    Map<String, String> getFieldMap();

}
