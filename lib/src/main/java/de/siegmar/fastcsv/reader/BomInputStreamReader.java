package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/// InputStreamReader that is capable of detecting and handling BOM headers.
final class BomInputStreamReader extends Reader {

    private final InputStream inputStream;
    private final Charset defaultCharset;
    private Reader reader;

    BomInputStreamReader(final InputStream inputStream, final Charset defaultCharset) {
        this.inputStream = inputStream;
        this.defaultCharset = defaultCharset;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (reader == null) {
            final var bomIn = new BomInputStream(inputStream, defaultCharset);
            reader = new InputStreamReader(bomIn, bomIn.getCharset());
        }

        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}
