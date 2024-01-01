package de.siegmar.fastcsv.reader;

/**
 * Simple mapper callback handler.
 *
 * @param <T> the type of the mapped object
 * @see CsvCallbackHandlers#forSimpleMapper(SimpleCsvMapper)
 */
@FunctionalInterface
public interface SimpleCsvMapper<T> {

    /**
     * Constructs a record of type {@code T} from the given fields.
     *
     * @param fields the fields
     * @return a record of type {@code T}
     */
    @SuppressWarnings("PMD.UseVarargs")
    T build(String[] fields);

}
