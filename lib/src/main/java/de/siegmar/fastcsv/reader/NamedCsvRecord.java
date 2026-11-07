package de.siegmar.fastcsv.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringJoiner;

import de.siegmar.fastcsv.util.Util;

/// Represents an immutable CSV record with named (and indexed) fields.
///
/// The field contents are never `null`. Empty fields are represented as empty strings.
///
/// Instances of this class are immutable and safe for use by multiple threads.
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

    /// Retrieves the field names of the header.
    ///
    /// The field names are returned in the order they appear in the CSV data.
    ///
    /// Note that the field names are not necessarily unique.
    /// If you need to collect all fields with the same field name, use [#getFieldsAsMapList()].
    ///
    /// Note that comments ([#isComment()]) and empty lines have an empty header.
    /// To retrieve the comment text, use [#getField(int)] with index 0.
    ///
    /// @return the field names, never `null`
    public List<String> getHeader() {
        // Not using List.of() here for performance reasons, as it copies the array.
        return Collections.unmodifiableList(Arrays.asList(header));
    }

    /// Retrieves the field content of a field by its case-sensitive field name, considering the first occurrence
    /// in case of duplicates.
    ///
    /// This method is equivalent to `findField(name).orElseThrow(NoSuchElementException::new)` although a
    /// more explanatory exception message is provided.
    ///
    /// @param name case-sensitive name of the field to be retrieved
    /// @return the field content, never `null`
    /// @throws NoSuchElementException if this record has no such field
    /// @throws NullPointerException   if `name` is `null`
    /// @see #findField(String)
    /// @see #findFields(String)
    public String getField(final String name) {
        final int fieldIdx = findHeaderIndex(name);

        // Check if the field index is valid
        if (fieldIdx == -1) {
            throw new NoSuchElementException(Util.escapeControlCharacters(
                "Header does not contain a field '%s'. Valid names are: %s".formatted(name, Arrays.toString(header))));
        }
        if (fieldIdx >= fields.length) {
            throw new NoSuchElementException(Util.escapeControlCharacters(
                "Field '%s' is on index %d, but current record only contains %d fields"
                    .formatted(name, fieldIdx, fields.length)));
        }

        // Return the field content
        return fields[fieldIdx];
    }

    // Finds the index for the first occurrence of the given field name (case-sensitive); returns -1 if not found
    private int findHeaderIndex(final String name) {
        for (int i = 0; i < header.length; i++) {
            if (name.equals(header[i])) {
                return i;
            }
        }
        return -1;
    }

    /// Retrieves the field content of a field by its case-sensitive field name, considering the first occurrence
    /// in case of duplicates.
    ///
    /// This method is equivalent to `findFields(name).stream().findFirst()` but does not collect all fields.
    ///
    /// @param name case-sensitive name of the field to be retrieved
    /// @return an [Optional] containing the field content if found,
    ///     or an empty [Optional] if the field is not present, never `null`
    /// @throws NullPointerException if `name` is `null`
    /// @see #findFields(String)
    public Optional<String> findField(final String name) {
        final int fieldIdx = findHeaderIndex(name);

        // Check if the field index is valid
        if (fieldIdx == -1 || fieldIdx >= fields.length) {
            return Optional.empty();
        }

        // Return the field content wrapped in an Optional
        return Optional.of(fields[fieldIdx]);
    }

    /// Collects the field contents of all fields with the given field name (case-sensitive) in the order they
    /// appear in the header.
    ///
    /// @param name case-sensitive name of the fields to collect
    /// @return the field contents (empty list if record doesn't contain that field), never `null`
    /// @throws NullPointerException if `name` is `null`
    public List<String> findFields(final String name) {
        final int bound = commonSize();
        final List<String> ret = new ArrayList<>(bound);
        for (int i = 0; i < bound; i++) {
            if (name.equals(header[i])) {
                ret.add(fields[i]);
            }
        }
        return ret;
    }

    /// Constructs an ordered map, associating field names with the corresponding field contents of this record,
    /// considering the first occurrence in case of duplicates.
    ///
    /// Only fields with a corresponding field name are mapped (and vice versa). No map entry has a
    /// `null` key or value.
    ///
    /// If you need to collect all fields with the same field name, use [#getFieldsAsMapList()].
    ///
    /// @return an ordered map of field names and field contents of this record, never `null`
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

    /// Constructs an unordered map, associating field names with an ordered list of corresponding field contents in
    /// this record.
    ///
    /// Only fields with a corresponding field name are mapped (and vice versa). No map entry has a
    /// `null` key or value.
    ///
    /// If you don't have to handle duplicate field names, you may simply use [#getFieldsAsMap()].
    ///
    /// @return an unordered map of field names and field contents of this record, never `null`
    /// @see #getFieldsAsMap()
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
    public Map<String, List<String>> getFieldsAsMapList() {
        final int bound = commonSize();
        final Map<String, List<String>> map = new HashMap<>(bound);
        for (int i = 0; i < bound; i++) {
            final String key = header[i];
            List<String> val = map.get(key);
            if (val == null) {
                val = new ArrayList<>();
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
