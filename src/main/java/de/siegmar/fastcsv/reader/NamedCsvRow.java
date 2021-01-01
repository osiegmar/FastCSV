package de.siegmar.fastcsv.reader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Name (header) based CSV-row.
 */
public final class NamedCsvRow {

    private final long originalLineNumber;
    private final Map<String, String> fieldMap;

    NamedCsvRow(final Set<String> header, final CsvRow row) {
        this.originalLineNumber = row.getOriginalLineNumber();

        fieldMap = new LinkedHashMap<>(header.size());
        int i = 0;
        for (final String h : header) {
            fieldMap.put(h, row.getField(i++));
        }
    }

    /**
     * Returns the original line number (starting with 1). On multi-line rows this is the starting
     * line number.
     * Empty lines (and maybe commented lines) have been skipped.
     *
     * @return the original line number
     */
    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    /**
     * Gets a field value by its name.
     *
     * @param name field name
     * @return field value, never {@code null}
     * @throws NoSuchElementException if this row has no such field
     */
    public String getField(final String name) {
        final String val = fieldMap.get(name);
        if (val == null) {
            throw new NoSuchElementException("No element with name '" + name + "' found. "
                + "Valid names are: " + fieldMap.keySet());
        }
        return val;
    }

    /**
     * Gets an unmodifiable map of header names and field values of this row.
     * <p>
     * The map will always contain all header names - even if their value is {@code null}.
     *
     * @return an unmodifiable map of header names and field values of this row
     */
    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fieldMap);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRow.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fieldMap=" + fieldMap)
            .toString();
    }

}
