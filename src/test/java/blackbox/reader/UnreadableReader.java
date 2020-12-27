package blackbox.reader;

import java.io.IOException;
import java.io.Reader;

class UnreadableReader extends Reader {

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        throw new IOException("Cannot read");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("Cannot close");
    }

}
