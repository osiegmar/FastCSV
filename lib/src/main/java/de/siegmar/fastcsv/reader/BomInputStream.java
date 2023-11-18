package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class BomInputStream extends InputStream {

    private static final Charset UTF32LE = Charset.forName("UTF-32LE");
    private static final Charset UTF32BE = Charset.forName("UTF-32BE");
    private static final int BOM_SIZE = 4;

    private final InputStream in;

    private byte[] buf;
    private int offset;

    BomInputStream(final InputStream inputStream) {
        in = inputStream;
    }

    /*
     * Optimized code to detect these BOM headers:
     *
     * UTF-8      : EF BB BF
     * UTF-16 (BE): FE FF
     * UTF-16 (LE): FF FE
     * UTF-32 (BE): 00 00 FE FF
     * UTF-32 (LE): FF FE 00 00
     */
    @SuppressWarnings({
        "checkstyle:MagicNumber",
        "checkstyle:CyclomaticComplexity",
        "checkstyle:BooleanExpressionComplexity",
        "PMD.AvoidLiteralsInIfCondition"
    })
    Optional<Charset> detectCharset() throws IOException {
        buf = in.readNBytes(BOM_SIZE);
        final int n = buf.length;

        if (n < 2) {
            return Optional.empty();
        }

        Charset charset = null;
        if (n > 2 && buf[0] == (byte) 0xEF) {
            if (buf[1] == (byte) 0xBB && buf[2] == (byte) 0xBF) {
                offset = 3;
                charset = StandardCharsets.UTF_8;
            }
        } else if (buf[0] == (byte) 0xFE) {
            if (buf[1] == (byte) 0xFF) {
                offset = 2;
                charset = StandardCharsets.UTF_16BE;
            }
        } else if (buf[0] == (byte) 0xFF && buf[1] == (byte) 0xFE) {
            if (n > 3 && buf[2] == (byte) 0x00 && buf[3] == (byte) 0x00) {
                offset = 4;
                charset = UTF32LE;
            } else {
                offset = 2;
                charset = StandardCharsets.UTF_16LE;
            }
        } else if (n > 3
            && buf[0] == (byte) 0x00 && buf[1] == (byte) 0x00
            && buf[2] == (byte) 0xFE && buf[3] == (byte) 0xFF) {
            offset = 4;
            charset = UTF32BE;
        }

        return Optional.ofNullable(charset);
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (offset < buf.length) {
            final int dataToCopy = Math.min(len, buf.length - offset);

            for (int i = 0; i < dataToCopy; i++) {
                b[off + i] = buf[offset++];
            }

            if (len > dataToCopy) {
                final int read = in.read(b, off + dataToCopy, len - dataToCopy);
                return read < 0 ? dataToCopy : dataToCopy + read;
            }

            return dataToCopy;
        }

        return in.read(b, off, len);
    }

}
