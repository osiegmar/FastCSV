package de.siegmar.fastcsv.reader;

/// A [CsvCallbackHandler] implementation that returns a [CsvRecord] for each record.
///
/// This implementation is stateful and must not be reused.
public final class CsvRecordHandler extends AbstractCsvCallbackHandler<CsvRecord> {

    /// Constructs a new [CsvRecordHandler].
    public CsvRecordHandler() {
        super();
    }

    /// Constructs a new [CsvRecordHandler] with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    public CsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    @Override
    protected RecordWrapper<CsvRecord> buildRecord() {
        return buildWrapper(new CsvRecord(startingLineNumber, compactFields(), comment));
    }

}
