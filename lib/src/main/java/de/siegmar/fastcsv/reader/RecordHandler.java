package de.siegmar.fastcsv.reader;

final class RecordHandler {

    private int len;
    private String[] record;
    private int idx;
    private int lines = 1;
    private boolean commentMode;
    private long originalLineNumber = 1;

    RecordHandler(final int len) {
        this.len = len;
        record = new String[len];
    }

    void add(final String value) {
        if (idx == len) {
            extendCapacity();
        }
        record[idx++] = value;
    }

    private void extendCapacity() {
        len *= 2;
        final String[] newRecord = new String[len];
        System.arraycopy(record, 0, newRecord, 0, idx);
        record = newRecord;
    }

    CsvRecord buildAndReset() {
        final CsvRecord csvRecord = idx > 0 ? build() : null;
        idx = 0;
        originalLineNumber += lines;
        lines = 1;
        commentMode = false;
        return csvRecord;
    }

    private CsvRecord build() {
        if (idx > 1 || !record[0].isEmpty()) {
            final String[] ret = new String[idx];
            System.arraycopy(record, 0, ret, 0, idx);
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
