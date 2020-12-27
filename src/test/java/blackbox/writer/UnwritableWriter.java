package blackbox.writer;

import java.io.IOException;
import java.io.Writer;

class UnwritableWriter extends Writer {

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        throw new IOException("Cannot write");
    }

    @Override
    public void flush() throws IOException {
        throw new IOException("Cannot flush");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("Cannot close");
    }

}
