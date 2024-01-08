package de.siegmar.fastcsv.reader;

/**
 * A {@link CsvCallbackHandler} implementation that returns a {@link CsvRecord} for each record.
 * <p>
 * This implementation is stateful and must not be reused.
 */
public final class CsvRecordHandler extends AbstractCsvCallbackHandler<CsvRecord> {

    /**
     * Constructs a new {@code CsvRecordHandler}.
     */
    public CsvRecordHandler() {
        super();
    }

    /**
     * Constructs a new {@code CsvRecordHandler} with the given field modifier.
     *
     * @param fieldModifier the field modifier, must not be {@code null}
     * @throws NullPointerException if {@code null} is passed
     */
    public CsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    @Override
    protected CsvRecord buildRecord(final String[] fields) {
        return new CsvRecord(startingLineNumber, fields, comment);
    }

}
