package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

final class BomInputStream extends InputStream {

    private final InputStream delegate;
    private final Charset charset;
    private final byte[] buffer = new byte[BomUtil.POTENTIAL_BOM_SIZE];
    private int remainingBuffer;

    BomInputStream(final InputStream inputStream, final Charset defaultCharset) throws IOException {
        delegate = inputStream;

        final int bufCnt = delegate.readNBytes(buffer, 0, BomUtil.POTENTIAL_BOM_SIZE);
        final Optional<BomHeader> optHeader = BomUtil.detectCharset(buffer);

        if (optHeader.isEmpty()) {
            remainingBuffer = bufCnt;
            charset = defaultCharset;
        } else {
            final BomHeader bomHeader = optHeader.get();
            remainingBuffer = bufCnt - bomHeader.getLength();
            charset = bomHeader.getCharset();
        }
    }

    Charset getCharset() {
        return charset;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return remainingBuffer > 0 ? readBuffer(b, off, len) : delegate.read(b, off, len);
    }

    @Override
    public int read() {
        // Have to implement this per contract, but it's not used within FastCSV
        throw new UnsupportedOperationException();
    }

    private int readBuffer(final byte[] b, final int off, final int len) {
        final int toCopy = Math.min(remainingBuffer, len);
        System.arraycopy(buffer, buffer.length - remainingBuffer, b, off, toCopy);
        remainingBuffer -= toCopy;
        return toCopy;
    }

}
