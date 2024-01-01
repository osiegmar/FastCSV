package de.siegmar.fastcsv.reader;

import java.util.List;
import java.util.Objects;

/**
 * Factory methods for default callback handlers.
 */
public final class CsvCallbackHandlers {

    private CsvCallbackHandlers() {
        // utility class
    }

    /**
     * Constructs a callback handler for {@link CsvRecord} record instances.
     *
     * @return a callback handler that returns a {@link CsvRecord} for each record
     */
    public static CsvCallbackHandler<CsvRecord> ofCsvRecord() {
        return new CsvRecordHandler();
    }

    /**
     * Constructs a callback handler for string array record instances.
     *
     * @return a callback handler that returns a string array for each record
     */
    public static CsvCallbackHandler<String[]> ofStringArray() {
        return new StringArrayHandler();
    }

    /**
     * Constructs a callback handler for {@link NamedCsvRecord} record instances.
     *
     * @return a callback handler that returns a {@link NamedCsvRecord} for each record
     */
    public static CsvCallbackHandler<NamedCsvRecord> ofNamedCsvRecord() {
        return new NamedCsvRecordHandler();
    }

    /**
     * Constructs a callback handler for {@link NamedCsvRecord} record instances.
     *
     * @param header the header names
     * @return a callback handler that returns a {@link NamedCsvRecord} for each record
     * @throws NullPointerException if {@code header} is {@code null}
     * @see #ofNamedCsvRecord(List)
     */
    public static CsvCallbackHandler<NamedCsvRecord> ofNamedCsvRecord(final String... header) {
        return new NamedCsvRecordHandler(Objects.requireNonNull(header, "header must not be null"));
    }

    /**
     * Constructs a callback handler for {@link NamedCsvRecord} record instances.
     *
     * @param header the header names
     * @return a callback handler that returns a {@link NamedCsvRecord} for each record
     * @throws NullPointerException if {@code header} is {@code null}
     * @see #ofNamedCsvRecord(String...)
     */
    public static CsvCallbackHandler<NamedCsvRecord> ofNamedCsvRecord(final List<String> header) {
        Objects.requireNonNull(header, "header must not be null");
        return new NamedCsvRecordHandler(header.toArray(new String[0]));
    }

    /**
     * Constructs a callback handler for the given {@link SimpleCsvMapper}.
     *
     * @param <T> the type of the resulting records
     * @param mapper the mapper
     * @return a callback handler that returns a mapped record for each record
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    public static <T> CsvCallbackHandler<T> forSimpleMapper(final SimpleCsvMapper<T> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return new AbstractCsvCallbackHandler<>() {
            @Override
            protected T buildRecord(final String[] fields) {
                return mapper.build(fields);
            }
        };
    }

}
