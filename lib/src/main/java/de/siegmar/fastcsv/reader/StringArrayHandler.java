package de.siegmar.fastcsv.reader;

/// A [CsvCallbackHandler] implementation that returns the fields of each record as an array of Strings.
///
/// This implementation is stateful and must not be reused.
public final class StringArrayHandler extends AbstractCsvCallbackHandler<String[]> {

    /// Constructs a new `StringArrayHandler`.
    public StringArrayHandler() {
        super();
    }

    /// Constructs a new `StringArrayHandler` with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    public StringArrayHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    @Override
    protected RecordWrapper<String[]> buildRecord() {
        return buildWrapper(compactFields());
    }

}
