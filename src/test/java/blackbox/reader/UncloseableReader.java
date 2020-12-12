package blackbox.reader;

import java.io.IOException;
import java.io.Reader;

class UncloseableReader extends Reader {

    private final Reader reader;

    UncloseableReader(final Reader reader) {
        this.reader = reader;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return reader.read();
    }

    @Override
    public void close() throws IOException {
        throw new IOException("Cannot close");
    }

}
