package de.siegmar.fastcsv.reader;

final class RecordHandler {

    private static final int INITIAL_FIELDS_SIZE = 32;
    private final FieldModifier fieldModifier;

    private int len;
    private String[] fields;
    private int idx;
    private int lines = 1;
    private boolean commentMode;
    private long originalLineNumber = 1;

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
    void add(String value) {
        if (idx == len) {
            extendCapacity();
        }
        if (fieldModifier != null) {
            value = fieldModifier.modify(originalLineNumber, idx, commentMode, value);
        }
        fields[idx++] = value;
    }

    void add(final char[] lBuf, final int lBegin, final int lPos) {
        add(new String(lBuf, lBegin, lPos));
    }

    private void extendCapacity() {
        len *= 2;
        final String[] newRecord = new String[len];
        System.arraycopy(fields, 0, newRecord, 0, idx);
        fields = newRecord;
    }

    CsvRecord buildAndReset() {
        final CsvRecord csvRecord = build();
        idx = 0;
        originalLineNumber += lines;
        lines = 1;
        commentMode = false;
        return csvRecord;
    }

    private CsvRecord build() {
        if (idx > 1 || !fields[0].isEmpty()) {
            final String[] ret = new String[idx];
            System.arraycopy(fields, 0, ret, 0, idx);
            return new CsvRecord(originalLineNumber, ret, commentMode);
        }

        return new CsvRecord(originalLineNumber, commentMode);
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

    public void setOriginalLineNumber(final long originalLineNumber) {
        this.originalLineNumber = originalLineNumber;
    }

}
