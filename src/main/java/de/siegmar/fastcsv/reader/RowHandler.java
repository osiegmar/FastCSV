package de.siegmar.fastcsv.reader;

final class RowHandler {

    private int len;
    private String[] row;
    private int idx;
    private int lines = 1;
    private boolean commentMode;
    private long originalLineNumber = 1;
    private long firstFieldStartOffset;

    RowHandler(final int len) {
        this.len = len;
        row = new String[len];
    }

    void add(final String value, final long startingOffset) {
        if (idx == 0) {
            firstFieldStartOffset = startingOffset;
        } else if (idx == len) {
            extendCapacity();
        }
        row[idx++] = value;
    }

    private void extendCapacity() {
        len *= 2;
        final String[] newRow = new String[len];
        System.arraycopy(row, 0, newRow, 0, idx);
        row = newRow;
    }

    CsvRow buildAndReset() {
        final CsvRow csvRow = idx > 0 ? build() : null;
        idx = 0;
        originalLineNumber += lines;
        lines = 1;
        commentMode = false;
        return csvRow;
    }

    private CsvRow build() {
        if (idx > 1 || !row[0].isEmpty()) {
            final String[] ret = new String[idx];
            System.arraycopy(row, 0, ret, 0, idx);
            return new CsvRow(originalLineNumber, firstFieldStartOffset, ret, commentMode);
        }

        return new CsvRow(originalLineNumber, firstFieldStartOffset, commentMode);
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

    public void reset() {
        originalLineNumber = 1;
    }

}
