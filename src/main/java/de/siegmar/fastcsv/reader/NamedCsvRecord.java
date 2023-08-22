package de.siegmar.fastcsv.reader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Name (header) based CSV-record.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class NamedCsvRecord {

    private final long originalLineNumber;
    private final Map<String, String> fieldMap;

    NamedCsvRecord(final Set<String> header, final CsvRecord csvRecord) {
        this.originalLineNumber = csvRecord.originalLineNumber();

        fieldMap = new LinkedHashMap<>(header.size());
        int i = 0;
        for (final String h : header) {
            fieldMap.put(h, csvRecord.field(i++));
        }
    }

    /**
     * Returns the original line number (starting with 1). On multi-line records this is the starting
     * line number.
     * Empty lines (and maybe commented lines) have been skipped.
     *
     * @return the original line number
     */
    public long originalLineNumber() {
        return originalLineNumber;
    }

    /**
     * Gets a field value by its name.
     *
     * @param name field name
     * @return field value, never {@code null}
     * @throws NoSuchElementException if this record has no such field
     */
    public String field(final String name) {
        final String val = fieldMap.get(name);
        if (val == null) {
            throw new NoSuchElementException("No element with name '" + name + "' found. "
                + "Valid names are: " + fieldMap.keySet());
        }
        return val;
    }

    /**
     * Gets an unmodifiable map of header names and field values of this record.
     * <p>
     * The map will always contain all header names - even if their value is {@code null}.
     *
     * @return an unmodifiable map of header names and field values of this record
     */
    public Map<String, String> fields() {
        return Collections.unmodifiableMap(fieldMap);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRecord.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fieldMap=" + fieldMap)
            .toString();
    }

}
