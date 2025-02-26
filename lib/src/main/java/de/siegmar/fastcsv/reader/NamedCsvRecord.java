package de.siegmar.fastcsv.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringJoiner;

/// Represents an immutable CSV record with named (and indexed) fields.
///
/// The field values are never `null`. Empty fields are represented as empty strings.
///
/// @see CsvReader
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public final class NamedCsvRecord extends CsvRecord {

    private final String[] header;

    @SuppressWarnings("PMD.UseVarargs")
    NamedCsvRecord(final long startingLineNumber, final String[] fields, final boolean comment,
                   final String[] header) {
        super(startingLineNumber, fields, comment);
        this.header = header;
    }

    /// Retrieves the header names of this record.
    ///
    /// The header names are returned in the order they appear in the CSV file.
    ///
    /// Note that the header names are not necessarily unique.
    /// If you need to collect all fields with the same name (duplicate header), use [#getFieldsAsMapList()].
    ///
    /// Note that records for commented lines ([#isComment()]) do not have an empty header.
    /// To retrieve the comment value, user [#getField(int)] with index 0.
    ///
    /// @return the header names, never `null`
    public List<String> getHeader() {
        return Collections.unmodifiableList(Arrays.asList(header));
    }

    /// Retrieves the value of a field by its case-sensitive name, considering the first occurrence in case of
    /// duplicates.
    ///
    /// This method is equivalent to `findField(name).orElseThrow(NoSuchElementException::new)` although a
    /// more explanatory exception message is provided.
    ///
    /// @param name case-sensitive name of the field to be retrieved
    /// @return field value, never `null`
    /// @throws NoSuchElementException if this record has no such field
    /// @throws NullPointerException   if name is `null`
    /// @see #findField(String)
    /// @see #findFields(String)
    public String getField(final String name) {
        final int fieldIdx = findHeaderIndex(name);

        // Check if the field index is valid
        if (fieldIdx == -1) {
            throw new NoSuchElementException(String.format(
                "Header does not contain a field '%s'. Valid names are: %s", name, Arrays.toString(header)));
        }
        if (fieldIdx >= fields.length) {
            throw new NoSuchElementException(String.format(
                "Field '%s' is on index %d, but current record only contains %d fields",
                name, fieldIdx, fields.length));
        }

        // Return the value of the field
        return fields[fieldIdx];
    }

    // Finds the index for the first occurrence of the given header name (case-sensitive); returns -1 if not found
    private int findHeaderIndex(final String name) {
        for (int i = 0; i < header.length; i++) {
            if (name.equals(header[i])) {
                return i;
            }
        }
        return -1;
    }

    /// Retrieves the value of a field by its case-sensitive name, considering the first occurrence in case of
    /// duplicates.
    ///
    /// This method is equivalent to `findFields(name).stream().findFirst()` but more performant.
    ///
    /// @param name case-sensitive name of the field to be retrieved
    /// @return An [Optional] containing the value of the field if found,
    ///     or an [Optional#EMPTY] if the field is not present. Never returns `null`.
    /// @throws NullPointerException if name is `null`
    /// @see #findFields(String)
    public Optional<String> findField(final String name) {
        final int fieldIdx = findHeaderIndex(name);

        // Check if the field index is valid
        if (fieldIdx == -1 || fieldIdx >= fields.length) {
            return Optional.empty();
        }

        // Return the value of the field wrapped in an Optional
        return Optional.of(fields[fieldIdx]);
    }

    /// Collects all field values with the given name (case-sensitive) in the order they appear in the header.
    ///
    /// @param name case-sensitive name of the field to collect values for
    /// @return the field values (empty list if record doesn't contain that field), never `null`
    /// @throws NullPointerException if name is `null`
    public List<String> findFields(final String name) {
        final int bound = header.length;
        final List<String> ret = new ArrayList<>(bound);
        for (int i = 0; i < bound; i++) {
            if (name.equals(header[i])) {
                ret.add(fields[i]);
            }
        }
        return ret;
    }

    /// Constructs an ordered map, associating header names with corresponding field values of this record,
    /// considering the first occurrence in case of duplicates.
    ///
    /// The constructed map will only contain entries for fields that have a key and a value. No map entry will have a
    /// `null` key or value.
    ///
    /// If you need to collect all fields with the same name (duplicate header), use [#getFieldsAsMapList()].
    ///
    /// @return an ordered map of header names and field values of this record, never `null`
    /// @see #getFieldsAsMapList()
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<String, String> getFieldsAsMap() {
        final int bound = commonSize();
        final Map<String, String> map = new LinkedHashMap<>(bound);
        for (int i = 0; i < bound; i++) {
            map.putIfAbsent(header[i], fields[i]);
        }
        return map;
    }

    /// Constructs an unordered map, associating header names with an ordered list of corresponding field values in
    /// this record.
    ///
    /// The constructed map will only contain entries for fields that have a key and a value. No map entry will have a
    /// `null` key or value.
    ///
    /// If you don't have to handle duplicate headers, you may simply use [#getFieldsAsMap()].
    ///
    /// @return an unordered map of header names and field values of this record, never `null`
    /// @see #getFieldsAsMap()
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
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

    // Mappings will only be created for fields that have a key and a value – return the minimum of both sizes
    private int commonSize() {
        return Math.min(header.length, fields.length);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvRecord.class.getSimpleName() + "[", "]")
            .add("startingLineNumber=" + startingLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .add("comment=" + comment)
            .add("header=" + Arrays.toString(header))
            .toString();
    }

}
