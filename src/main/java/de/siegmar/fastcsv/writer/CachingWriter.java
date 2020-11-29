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
        if (pos == BUFFER_SIZE) {
            flushBuffer();
        }
        buf[pos++] = c;
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    public void write(final String str, final int off, final int len) throws IOException {
        if (pos + len >= BUFFER_SIZE) {
            flushBuffer();
            writer.write(str, off, len);
        } else {
            str.getChars(off, off + len, buf, pos);
            pos += len;
        }
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
