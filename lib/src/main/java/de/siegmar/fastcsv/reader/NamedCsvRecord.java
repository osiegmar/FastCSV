package de.siegmar.fastcsv.reader;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Name (header) based CSV-record.
 */
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public final class NamedCsvRecord extends CsvRecord {

    private final String[] header;

    NamedCsvRecord(final String[] header, final CsvRecord csvRecord) {
        super(csvRecord);
        this.header = header;
    }

    /**
     * Gets a field value by its name (first occurrence if duplicates exists).
     *
     * @param name field name
     * @return field value, never {@code null}
     * @throws NoSuchElementException if this record has no such field
     * @throws NullPointerException   if name is {@code null}
     * @see #findField(String)
     * @see #findFields(String)
     */
    public String getField(final String name) {
        final int fieldPos = findHeader(name);
        if (fieldPos == -1) {
            throw new NoSuchElementException(MessageFormat.format(
                "Header does not contain a field ''{0}''. Valid names are: {1}", name, Arrays.toString(header)));
        } else if (fieldPos >= fields.length) {
            throw new NoSuchElementException(MessageFormat.format(
                "Field ''{0}'' is on position {1}, but current record only contains {2} fields",
                name, fieldPos + 1, fields.length));
        }
        return fields[fieldPos];
    }

    private int findHeader(final String name) {
        for (int i = 0; i < header.length; i++) {
            final String h = header[i];
            if (name.equals(h)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Finds a field value by its name (first occurrence if duplicates exists).
     *
     * @param name field name
     * @return the field value ({@link Optional#empty()} if record doesn't contain that field), never {@code null}
     * @throws NullPointerException if name is {@code null}
     * @see #findFields(String)
     */
    public Optional<String> findField(final String name) {
        final int fieldPos = findHeader(name);
        if (fieldPos == -1 || fieldPos >= fields.length) {
            return Optional.empty();
        }
        return Optional.of(fields[fieldPos]);
    }

    /**
     * Builds a list of field values found by its name.
     *
     * @param name field name
     * @return the field values (empty list if record doesn't contain that field), never {@code null}
     * @throws NullPointerException if name is {@code null}
     */
    public List<String> findFields(final String name) {
        final List<String> ret = new ArrayList<>();
        final int bound = header.length;
        for (int i = 0; i < bound; i++) {
            if (name.equals(header[i])) {
                ret.add(fields[i]);
            }
        }
        return ret;
    }

    /**
     * Builds an ordered map of header names and field values (first occurrence if duplicates exists)
     * of this record.
     * <p>
     * The map will contain only entries for fields that have a key and a value –
     * no map entry will have a {@code null} key or value.
     *
     * @return an ordered map of header names and field values of this record, never {@code null}
     */
    public Map<String, String> getFieldsAsMap() {
        final int bound = commonSize();
        final Map<String, String> map = new LinkedHashMap<>(bound);
        for (int i = 0; i < bound; i++) {
            map.putIfAbsent(header[i], fields[i]);
        }
        return map;
    }

    /**
     * Builds an unordered map of header names and field values of this record.
     * <p>
     * The map will contain only entries for fields that have a key and a value –
     * no map entry will have a {@code null} key or value.
     *
     * @return an unordered map of header names and field values of this record, never {@code null}
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, List<String>> getFieldsAsMapList() {
        final int bound = commonSize();
        final Map<String, List<String>> map = new HashMap<>(bound);
        for (int i = 0; i < bound; i++) {
            final String key = header[i];
            List<String> val = map.get(key);
            if (val == null) {
                val = new LinkedList<>();
                map.put(key, val);
            }
            val.add(fields[i]);
        }
        return map;
    }

    private int commonSize() {
        return Math.min(header.length, fields.length);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRecord.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .add("header=" + Arrays.toString(header))
            .toString();
    }

}
