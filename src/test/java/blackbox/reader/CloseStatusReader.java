package blackbox.reader;

import java.io.IOException;
import java.io.Reader;

class CloseStatusReader extends Reader {

    private final Reader reader;
    private boolean closed;

    CloseStatusReader(final Reader reader) {
        this.reader = reader;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return reader.read();
    }

    @Override
    public void close() throws IOException {
        reader.close();
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

}
