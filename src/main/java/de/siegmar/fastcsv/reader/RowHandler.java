package de.siegmar.fastcsv.reader;

final class RowHandler {

    private static final String[] EMPTY = new String[0];

    private int len;
    private String[] row;
    private int idx;
    private int lines = 1;
    private boolean commentMode;

    RowHandler(final int len) {
        this.len = len;
        row = new String[len];
    }

    void add(final String value) {
        if (idx == len) {
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

    String[] endAndReset() {
        lines = 1;
        commentMode = false;

        if (idx == 0) {
            return EMPTY;
        }

        final String[] ret = new String[idx];
        System.arraycopy(row, 0, ret, 0, idx);
        idx = 0;
        return ret;
    }

    public void enableCommentMode() {
        commentMode = true;
    }

    public boolean isCommentMode() {
        return commentMode;
    }

    long getLines() {
        return lines;
    }

    public void incLines() {
        lines++;
    }

}
