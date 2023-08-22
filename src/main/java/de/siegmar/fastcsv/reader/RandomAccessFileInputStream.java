package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class RandomAccessFileInputStream extends InputStream {

    private final RandomAccessFile raf;

    RandomAccessFileInputStream(final RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

}
