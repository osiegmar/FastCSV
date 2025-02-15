package de.siegmar.fastcsv.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
    void bomOnly() throws IOException {
        assertThat(read(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}))
            .isEmpty();
    }

    @Test
    void dataAfterBom() throws IOException {
        assertThat(read(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 0}))
            .isEqualTo(new byte[]{0});
    }

    private byte[] read(final byte[] src) throws IOException {
        return new BomInputStream(new ByteArrayInputStream(src), UTF_8).readAllBytes();
    }

    @Test
    void noSingleRead() {
        assertThatThrownBy(() -> new BomInputStream(new ByteArrayInputStream(new byte[0]), UTF_8).read())
            .isInstanceOf(UnsupportedOperationException.class);
    }

}
