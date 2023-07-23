package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class UnicodeReader extends Reader { // FIXME extend FilterReader?

    private static final Charset UTF_32LE = Charset.forName("UTF-32LE");
    private static final Charset UTF_32BE = Charset.forName("UTF-32BE");
    private static final int BOM_SIZE = 4;

    private final InputStreamReader reader;

    UnicodeReader(final InputStream in, final Charset defaultCharset) throws IOException {
        final byte[] bom = new byte[BOM_SIZE];

        final PushbackInputStream pushbackStream = new PushbackInputStream(in, BOM_SIZE);
        final int n = pushbackStream.read(bom, 0, bom.length);

        Charset encoding = defaultCharset;
        int len = -1;

        if (n > 1) {
            if (n > 2 && bom[0] == (byte) 0xEF) {
                if (bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                    encoding = StandardCharsets.UTF_8;
                    len = 3;
                }
            } else if (bom[0] == (byte) 0xFE) {
                if (bom[1] == (byte) 0xFF) {
                    encoding = StandardCharsets.UTF_16BE;
                    len = 2;
                }
            } else if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                if (n == 4 && bom[2] == (byte) 0x00 && bom[3] == (byte) 0x00) {
                    encoding = UTF_32LE;
                    len = 4;
                } else {
                    encoding = StandardCharsets.UTF_16LE;
                    len = 2;
                }
            } else if (n == 4
                    && bom[0] == (byte) 0x00 && bom[1] == (byte) 0x00
                    && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
                encoding = UTF_32BE;
                len = 4;
            }
        }

        if (len < 0) {
            pushbackStream.unread(bom, 0, n);
        } else if (len < n) {
            pushbackStream.unread(bom, len, n - len);
        }

        reader = new InputStreamReader(pushbackStream, encoding);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
