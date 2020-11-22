package de.siegmar.fastcsv.writer;

import java.io.IOException;
import java.io.Writer;

/**
 * Unsynchronized and thus high performance replacement for BufferedWriter.
 * <p>
 * This class is intended for internal use only.
 */
final class CachingWriter {

    private static final int BUFFER_SIZE = 8192;

    private final Writer writer;
    private final char[] buf = new char[BUFFER_SIZE];
    private int pos;

    CachingWriter(final Writer writer) {
        this.writer = writer;
    }

    public void write(final char c) throws IOException {
        buf[pos++] = c;
        if (pos >= BUFFER_SIZE) {
            flushBuffer();
        }
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    public void write(final String str, int off, int len) throws IOException {
        do {
            final int copyLen = Math.min(BUFFER_SIZE - pos, len);
            str.getChars(off, off + copyLen, buf, pos);
            pos += copyLen;
            off += copyLen;
            len -= copyLen;
            if (pos >= BUFFER_SIZE) {
                flushBuffer();
            }
        } while (len > 0);
    }

    public void flushBuffer() throws IOException {
        writer.write(buf, 0, pos);
        pos = 0;
    }

    public void close() throws IOException {
        flushBuffer();
        writer.close();
    }

}
