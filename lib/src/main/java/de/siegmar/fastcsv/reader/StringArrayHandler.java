package de.siegmar.fastcsv.reader;

/**
 * A {@link CsvCallbackHandler} implementation that returns the fields of each record as an array of Strings.
 * <p>
 * This implementation is stateful and must not be reused.
 */
public final class StringArrayHandler extends AbstractCsvCallbackHandler<String[]> {

    /**
     * Constructs a new {@code StringArrayHandler}.
     */
    public StringArrayHandler() {
        super();
    }

    /**
     * Constructs a new {@code StringArrayHandler} with the given field modifier.
     *
     * @param fieldModifier the field modifier, must not be {@code null}
     * @throws NullPointerException if {@code null} is passed
     */
    public StringArrayHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    @Override
    protected RecordWrapper<String[]> buildRecord() {
        return buildWrapper(compactFields());
    }

}
