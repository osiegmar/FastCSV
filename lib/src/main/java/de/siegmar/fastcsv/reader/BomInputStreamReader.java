package de.siegmar.fastcsv.reader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class BomInputStreamReader extends Reader {

    private static final Charset UTF32LE = Charset.forName("UTF-32LE");
    private static final Charset UTF32BE = Charset.forName("UTF-32BE");
    private static final int BOM_SIZE = 4;

    private final InputStream in;
    private final Charset defaultCharset;

    private InputStreamReader r;

    BomInputStreamReader(final InputStream in, final Charset defaultCharset) {
        this.in = new BufferedInputStream(in);
        this.defaultCharset = defaultCharset;
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
        "checkstyle:NestedIfDepth",
        "PMD.AvoidLiteralsInIfCondition"
    })
    Optional<Charset> detectCharset() throws IOException {
        in.mark(BOM_SIZE);
        final byte[] buf = in.readNBytes(BOM_SIZE);
        final int n = buf.length;

        if (n < 2) {
            in.reset();
            return Optional.empty();
        }

        Charset charset = null;
        int bomLen = 0;

        if (buf[0] == (byte) 0xEF) {
            if (n > 2 && buf[1] == (byte) 0xBB && buf[2] == (byte) 0xBF) {
                charset = StandardCharsets.UTF_8;
                bomLen = 3;
            }
        } else if (buf[0] == (byte) 0xFE) {
            if (buf[1] == (byte) 0xFF) {
                charset = StandardCharsets.UTF_16BE;
                bomLen = 2;
            }
        } else if (buf[0] == (byte) 0xFF) {
            if (buf[1] == (byte) 0xFE) {
                if (n > 3 && buf[2] == (byte) 0x00 && buf[3] == (byte) 0x00) {
                    charset = UTF32LE;
                    bomLen = 4;
                } else {
                    charset = StandardCharsets.UTF_16LE;
                    bomLen = 2;
                }
            }
        } else if (buf[0] == (byte) 0x00) {
            if (n > 3
                && buf[1] == (byte) 0x00
                && buf[2] == (byte) 0xFE
                && buf[3] == (byte) 0xFF) {
                bomLen = 4;
                charset = UTF32BE;
            }
        }

        if (bomLen < n) {
            in.reset();
            if (in.skip(bomLen) != bomLen) {
                throw new IOException("BOM header couldn't be skipped");
            }
        }

        return Optional.ofNullable(charset);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (r == null) {
            r = new InputStreamReader(in, detectCharset().orElse(defaultCharset));
        }

        return r.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}