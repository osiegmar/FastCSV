package de.siegmar.fastcsv.reader;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

/**
 * Name (header) based CSV-record.
 */
public final class NamedCsvRecord {

    private final long originalLineNumber;
    private final List<String> header;
    private final CsvRecord csvRecord;

    NamedCsvRecord(final List<String> header, final CsvRecord csvRecord) {
        this.originalLineNumber = csvRecord.getOriginalLineNumber();
        this.header = header;
        this.csvRecord = csvRecord;
    }

    /**
     * Returns the original line number (starting with 1). On multi-line records this is the starting
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
     * @throws NoSuchElementException if this record has no such field
     */
    public String getField(final String name) {
        final int fieldPos = header.indexOf(name);
        if (fieldPos == -1) {
            throw new NoSuchElementException(MessageFormat.format(
                "Header does not contain a field ''{0}''. Valid names are: {1}", name, header));
        } else if (fieldPos >= csvRecord.getFieldCount()) {
            throw new NoSuchElementException(MessageFormat.format(
                "Field ''{0}'' is on position {1}, but current record only contains {2} fields",
                name, fieldPos + 1, csvRecord.getFieldCount()));
        }
        return csvRecord.getField(fieldPos);
    }

    /**
     * Gets an unmodifiable map of header names and field values of this record.
     * <p>
     * The map will always contain all header names - even if their value is {@code null}.
     *
     * @return an unmodifiable map of header names and field values of this record
     */
    public Map<String, String> getFieldsAsMap() {
        final int size = Math.min(header.size(), csvRecord.getFieldCount());
        final Map<String, String> fieldMap = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            fieldMap.put(header.get(i), csvRecord.getField(i));
        }
        return Collections.unmodifiableMap(fieldMap);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRecord.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fields=" + csvRecord.getFields())
            .add("header=" + header)
            .toString();
    }

}
