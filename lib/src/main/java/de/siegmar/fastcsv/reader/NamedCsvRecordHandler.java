package de.siegmar.fastcsv.reader;

import java.util.List;
import java.util.Objects;

/// A callback handler that returns a [NamedCsvRecord] for each record.
///
/// This implementation is stateful and must not be reused.
public final class NamedCsvRecordHandler extends AbstractCsvCallbackHandler<NamedCsvRecord> {

    private static final String[] EMPTY_HEADER = new String[0];
    private String[] header;

    /// Constructs a new [NamedCsvRecordHandler] with an empty header.
    public NamedCsvRecordHandler() {
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header.
    ///
    /// @param header the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    public NamedCsvRecordHandler(final List<String> header) {
        setHeader(header.toArray(new String[0]));
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header.
    ///
    /// @param header the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    public NamedCsvRecordHandler(final String... header) {
        setHeader(header);
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    public NamedCsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header and field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @param header        the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    public NamedCsvRecordHandler(final FieldModifier fieldModifier, final List<String> header) {
        super(fieldModifier);
        setHeader(header.toArray(new String[0]));
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header and field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @param header        the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    public NamedCsvRecordHandler(final FieldModifier fieldModifier, final String... header) {
        super(fieldModifier);
        setHeader(header);
    }

    private void setHeader(final String... header) {
        Objects.requireNonNull(header, "header must not be null");
        for (final String h : header) {
            Objects.requireNonNull(h, "header must not be null");
        }
        this.header = header.clone();
    }

    @Override
    protected RecordWrapper<NamedCsvRecord> buildRecord() {
        if (comment) {
            return buildWrapper(new NamedCsvRecord(startingLineNumber, compactFields(), true, EMPTY_HEADER));
        }

        if (header == null) {
            setHeader(compactFields());
            return null;
        }

        return buildWrapper(new NamedCsvRecord(startingLineNumber, compactFields(), false, header));
    }

}
