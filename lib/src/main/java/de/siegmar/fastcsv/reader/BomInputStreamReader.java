package de.siegmar.fastcsv.reader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Optional;

class BomInputStreamReader extends Reader {

    private final InputStream in;
    private final Charset defaultCharset;

    private InputStreamReader r;

    BomInputStreamReader(final InputStream in, final Charset defaultCharset) {
        this.in = new BufferedInputStream(in);
        this.defaultCharset = defaultCharset;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (r == null) {
            r = new InputStreamReader(in, detectCharset(in).orElse(defaultCharset));
        }

        return r.read(cbuf, off, len);
    }

    private static Optional<Charset> detectCharset(final InputStream in) throws IOException {
        // Read potential BOM header
        final var optionalBomHeader = BomDetector.detectCharset(peakHeader(in));

        // No BOM header found
        if (optionalBomHeader.isEmpty()) {
            return Optional.empty();
        }

        // Skip BOM header
        final var bomHeader = optionalBomHeader.get();
        final int bomLength = bomHeader.getLength();
        if (in.skip(bomLength) != bomLength) {
            throw new IOException("Couldn't skip BOM header");
        }

        // Return charset
        return Optional.of(bomHeader.getCharset());
    }

    private static byte[] peakHeader(final InputStream in) throws IOException {
        in.mark(BomDetector.POTENTIAL_BOM_SIZE);
        final byte[] read = in.readNBytes(BomDetector.POTENTIAL_BOM_SIZE);
        in.reset();
        return read;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
