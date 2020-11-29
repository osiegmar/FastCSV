package de.siegmar.fastcsv.reader;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * This class represents a single CSV row.
 */
final class CsvRowImpl implements CsvRow {

    /**
     * The original line number (empty lines may be skipped).
     */
    private final long originalLineNumber;

    private final String[] fields;
    private final boolean comment;

    CsvRowImpl(final long originalLineNumber, final String[] fields, final boolean comment) {
        this.originalLineNumber = originalLineNumber;
        this.fields = fields;
        this.comment = comment;
    }

    @Override
    public long getOriginalLineNumber() {
        return originalLineNumber;
    }

    @Override
    public String getField(final int index) {
        return fields[index];
    }

    @Override
    public String[] getFields() {
        return fields.clone();
    }

    @Override
    public int getFieldCount() {
        return fields.length;
    }

    @Override
    public boolean isComment() {
        return comment;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvRowImpl.class.getSimpleName() + "[", "]")
            .add("originalLineNumber=" + originalLineNumber)
            .add("fields=" + Arrays.toString(fields))
            .toString();
    }

}
