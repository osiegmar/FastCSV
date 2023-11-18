package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BomInputStreamTest {

    @Test
    void noData() throws IOException {
        final byte[] data = {};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset()).isEmpty();

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, readData.length);
        assertThat(read).isEqualTo(-1);
        assertThat(readData).containsExactly(0, 0, 0, 0, 0);
    }

    @Test
    void fewData() throws IOException {
        final byte[] data = {1};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset()).isEmpty();

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, readData.length);
        assertThat(read).isOne();
        assertThat(readData).containsExactly(1, 0, 0, 0, 0);
    }

    @Test
    void noBom() throws IOException {
        final byte[] data = {1, 2, 3, 4, 5};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset()).isEmpty();

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, readData.length);
        assertThat(read).isEqualTo(5);
        assertThat(readData).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void onlyBom() throws IOException {
        final byte[] data = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset())
            .contains(StandardCharsets.UTF_8);

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, readData.length);
        assertThat(read).isEqualTo(-1);
        assertThat(readData).containsExactly(0, 0, 0, 0, 0);
    }

    @Test
    void partOfBomBuffer() throws IOException {
        final byte[] data = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 1, 2, 3};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset())
            .contains(StandardCharsets.UTF_8);

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, 1);
        assertThat(read).isOne();
        assertThat(readData).containsExactly(1, 0, 0, 0, 0);
    }

    @Test
    void dataAfterBom() throws IOException {
        final byte[] data = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 1, 2, 3};
        final var bis = new BomInputStream(new ByteArrayInputStream(data));

        assertThat(bis.detectCharset())
            .contains(StandardCharsets.UTF_8);

        final byte[] readData = new byte[5];
        final int read = bis.read(readData, 0, readData.length);
        assertThat(read).isEqualTo(3);
        assertThat(readData).containsExactly(1, 2, 3, 0, 0);
    }

}
