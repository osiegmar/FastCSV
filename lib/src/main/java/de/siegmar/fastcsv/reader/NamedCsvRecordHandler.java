package de.siegmar.fastcsv.reader;

import java.util.List;
import java.util.Objects;

/**
 * A callback handler that returns a {@link NamedCsvRecord} for each record.
 * <p>
 * This implementation is stateful and must not be reused.
 */
public final class NamedCsvRecordHandler extends AbstractCsvCallbackHandler<NamedCsvRecord> {

    private static final String[] EMPTY_HEADER = new String[0];
    private String[] header;

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with an empty header.
     */
    public NamedCsvRecordHandler() {
    }

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with the given header.
     *
     * @param header        the header, must not be {@code null} or contain {@code null} elements
     * @throws NullPointerException if {@code null} is passed
     */
    public NamedCsvRecordHandler(final List<String> header) {
        setHeader(header.toArray(new String[0]));
    }

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with the given header.
     *
     * @param header        the header, must not be {@code null} or contain {@code null} elements
     * @throws NullPointerException if {@code null} is passed
     */
    public NamedCsvRecordHandler(final String... header) {
        setHeader(header);
    }

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with the given field modifier.
     *
     * @param fieldModifier the field modifier, must not be {@code null}
     * @throws NullPointerException if {@code null} is passed
     */
    public NamedCsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with the given header and field modifier.
     *
     * @param fieldModifier the field modifier, must not be {@code null}
     * @param header        the header, must not be {@code null} or contain {@code null} elements
     * @throws NullPointerException if {@code null} is passed
     */
    public NamedCsvRecordHandler(final FieldModifier fieldModifier, final List<String> header) {
        super(fieldModifier);
        setHeader(header.toArray(new String[0]));
    }

    /**
     * Constructs a new {@code NamedCsvRecordHandler} with the given header and field modifier.
     *
     * @param fieldModifier the field modifier, must not be {@code null}
     * @param header        the header, must not be {@code null} or contain {@code null} elements
     * @throws NullPointerException if {@code null} is passed
     */
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
    protected NamedCsvRecord buildRecord(final String[] fields) {
        if (comment) {
            return new NamedCsvRecord(startingLineNumber, fields, true, EMPTY_HEADER);
        }

        if (header == null) {
            setHeader(fields);
            return null;
        }

        return new NamedCsvRecord(startingLineNumber, fields, false, header);
    }

}
