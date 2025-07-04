package de.siegmar.fastcsv.writer;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/// High-performance buffered writer (without synchronization).
class FastBufferedWriter extends FilterWriter implements Writable {

    private final char[] buf;
    private int pos;

    FastBufferedWriter(final Writer writer, final int bufferSize) {
        super(writer);
        buf = new char[bufferSize];
    }

    @Override
    public void write(final int c) throws IOException {
        if (pos == buf.length) {
            flushBuffer();
        }
        buf[pos++] = (char) c;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        if (pos + len >= buf.length) {
            flushBuffer();
            if (len >= buf.length) {
                out.write(cbuf, off, len);
                return;
            }
        }

        System.arraycopy(cbuf, off, buf, pos, len);
        pos += len;
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        if (pos + len >= buf.length) {
            flushBuffer();
            if (len >= buf.length) {
                out.write(str, off, len);
                return;
            }
        }

        str.getChars(off, off + len, buf, pos);
        pos += len;
    }

    @Override
    public void endRecord() throws IOException {
    }

    private void flushBuffer() throws IOException {
        out.write(buf, 0, pos);
        pos = 0;
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        super.close();
    }

}
