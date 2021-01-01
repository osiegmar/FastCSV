package de.siegmar.fastcsv;

import java.io.Reader;

class InfiniteDataReader extends Reader {

    private final char[] data;
    private int pos;

    InfiniteDataReader(final String data) {
        this.data = data.toCharArray();
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) {
        int copied = 0;
        while (copied < len) {
            final int tlen = Math.min(len - copied, data.length - pos);
            System.arraycopy(data, pos, cbuf, off + copied, tlen);
            copied += tlen;
            pos += tlen;

            if (pos == data.length) {
                pos = 0;
            }
        }

        return copied;
    }

    @Override
    public void close() {
        // NOP
    }

}
