package de.siegmar.fastcsv.reader;

/**
 * Simple mapper callback handler.
 *
 * @param <T> the type of the mapped object
 * @see CsvCallbackHandler#forSimpleMapper(SimpleCsvMapper)
 */
@FunctionalInterface
public interface SimpleCsvMapper<T> {

    /**
     * Maps the given fields to an object of type {@code T}.
     *
     * @param fields the fields
     * @return a record of type {@code T}
     */
    @SuppressWarnings("PMD.UseVarargs")
    T map(String[] fields);

}
