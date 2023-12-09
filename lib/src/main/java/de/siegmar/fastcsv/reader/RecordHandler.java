package de.siegmar.fastcsv.reader;

import java.text.MessageFormat;

import de.siegmar.fastcsv.util.Limits;

final class RecordHandler {

    private static final int INITIAL_FIELDS_SIZE = 32;

    private final FieldModifier fieldModifier;

    private int len;
    private String[] fields;
    private int recordSize;
    private int idx;
    private int lines = 1;
    private boolean commentMode;
    private long startingLineNumber = 1;

    RecordHandler(final FieldModifier fieldModifier) {
        this(INITIAL_FIELDS_SIZE, fieldModifier);
    }

    RecordHandler(final int len, final FieldModifier fieldModifier) {
        this.len = len;
        fields = new String[len];
        this.fieldModifier = fieldModifier;
    }

    @SuppressWarnings({
        "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment",
        "PMD.AvoidReassigningParameters"
    })
    void add(String value, final boolean quoted) {
        if (idx == len) {
            extendCapacity();
        }

        if (fieldModifier != null) {
            value = fieldModifier.modify(startingLineNumber, idx, commentMode, quoted, value);
            if (value == null) {
                throw new NullPointerException("fieldModifier returned illegal null: " + fieldModifier.getClass());
            }
        }

        fields[idx++] = value;
        recordSize += value.length();
        if (recordSize > Limits.MAX_RECORD_SIZE) {
            throw new CsvParseException(MessageFormat.format(
                "Record starting at line {0} has surpassed the maximum limit of {1} characters",
                startingLineNumber, Limits.MAX_RECORD_SIZE));
        }
    }

    private void extendCapacity() {
        len *= 2;
        if (len > Limits.MAX_FIELD_COUNT) {
            throw new CsvParseException("Maximum number of fields exceeded: " + Limits.MAX_FIELD_COUNT);
        }
        final String[] newFields = new String[len];
        System.arraycopy(fields, 0, newFields, 0, idx);
        fields = newFields;
    }

    CsvRecord buildAndReset() {
        final CsvRecord csvRecord = build();
        idx = 0;
        startingLineNumber += lines;
        lines = 1;
        commentMode = false;
        recordSize = 0;
        return csvRecord;
    }

    private CsvRecord build() {
        if (idx <= 1 && fields[0].isEmpty()) {
            // empty record
            return new CsvRecord(startingLineNumber, commentMode);
        }

        final String[] ret = new String[idx];
        System.arraycopy(fields, 0, ret, 0, idx);
        return new CsvRecord(startingLineNumber, ret, commentMode);
    }

    public void enableCommentMode() {
        commentMode = true;
    }

    public boolean isCommentMode() {
        return commentMode;
    }

    public void incLines() {
        lines++;
    }

    public void setStartingLineNumber(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
    }

}
