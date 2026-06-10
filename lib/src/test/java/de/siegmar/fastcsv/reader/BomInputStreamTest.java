package de.siegmar.fastcsv.reader;

import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

class BomInputStreamTest {

    @Test
    void empty() throws IOException {
        assertThat(read(new byte[]{}))
            .isEmpty();
    }

    @Test
    void unchanged() throws IOException {
        assertThat(read(new byte[]{0, 1, 2, 3, 4, 5}))
            .isEqualTo(new byte[]{0, 1, 2, 3, 4, 5});
    }

    @Test
    void shortInputWithoutBom() throws IOException {
        assertThat(read(new byte[]{1, 2, 3}))
            .isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void bomOnly() throws IOException {
        assertThat(read(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}))
            .isEmpty();
    }

    @Test
    void dataAfterBom() throws IOException {
        assertThat(read(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0}))
            .isEqualTo(new byte[]{0});
    }

    @Test
    void truncatedUtf16LeBomNotMisdetectedAsUtf32Le() throws IOException {
        // A stream of exactly FF FE must be detected as UTF-16 LE, not zero-padded to
        // FF FE 00 00 and misdetected as UTF-32 LE.
        assertThat(charset(new byte[]{(byte) 0xFF, (byte) 0xFE}))
            .isEqualTo(UTF_16LE);
    }

    private byte[] read(final byte[] src) throws IOException {
        return new BomInputStream(new ByteArrayInputStream(src), UTF_8).readAllBytes();
    }

    private Charset charset(final byte[] src) throws IOException {
        return new BomInputStream(new ByteArrayInputStream(src), UTF_8).getCharset();
    }

    @Test
    void noSingleRead() {
        assertThatThrownBy(() -> new BomInputStream(new ByteArrayInputStream(new byte[0]), UTF_8).read())
            .isInstanceOf(UnsupportedOperationException.class);
    }

}
