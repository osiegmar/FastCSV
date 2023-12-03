package de.siegmar.fastcsv.reader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

enum BomHeader {

    UTF_8(StandardCharsets.UTF_8, 3),
    UTF_16_BE(StandardCharsets.UTF_16BE, 2),
    UTF_16_LE(StandardCharsets.UTF_16LE, 2),
    UTF_32_BE(Charset.forName("UTF-32BE"), 4),
    UTF_32_LE(Charset.forName("UTF-32LE"), 4);

    private final Charset charset;
    private final int length;

    BomHeader(final Charset charset, final int length) {
        this.charset = charset;
        this.length = length;
    }

    Charset getCharset() {
        return charset;
    }

    int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BomHeader.class.getSimpleName() + "[", "]")
            .add("charset=" + charset)
            .add("length=" + length)
            .toString();
    }

}
