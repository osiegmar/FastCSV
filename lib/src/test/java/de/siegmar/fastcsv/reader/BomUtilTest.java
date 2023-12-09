package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BomUtilTest {

    @ParameterizedTest
    @MethodSource
    void charset(final String charset, final int len, final byte[] data) {
        if (charset == null) {
            assertThat(BomUtil.detectCharset(data)).isEmpty();
        } else {
            assertThat(BomUtil.detectCharset(data))
                .get()
                .satisfies(
                    bh -> assertThat(bh.getCharset()).hasToString(charset),
                    bh -> assertThat(bh.getLength()).isEqualTo(len),
                    bh -> assertThat(bh.toString()).isEqualTo("BomHeader[charset=%s, length=%s]", charset, len)
                );
        }
    }

    static Stream<Arguments> charset() {
        // @formatter:off
        return Stream.of(
            arguments(null, 0,       new byte[] {(byte) 0xEF}),
            arguments(null, 0,       new byte[] {(byte) 0xEF, 0}),
            arguments(null, 0,       new byte[] {(byte) 0xEF, (byte) 0xBB}),
            arguments(null, 0,       new byte[] {(byte) 0xEF, (byte) 0xBB, 0}),
            arguments(null, 0,       new byte[] {(byte) 0xEF, 0, (byte) 0xBF}),
            arguments("UTF-8", 3,    new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}),
            arguments(null, 0,       new byte[] {(byte) 0xFE, 0}),
            arguments("UTF-16BE", 2, new byte[] {(byte) 0xFE, (byte) 0xFF}),
            arguments("UTF-16BE", 2, new byte[] {(byte) 0xFE, (byte) 0xFF, 0}),
            arguments(null, 0,       new byte[] {(byte) 0xFF, 0}),
            arguments(null, 0,       new byte[] {0, (byte) 0xFF}),
            arguments("UTF-16LE", 2, new byte[] {(byte) 0xFF, (byte) 0xFE, 0}),
            arguments("UTF-16LE", 2, new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 1}),
            arguments("UTF-16LE", 2, new byte[] {(byte) 0xFF, (byte) 0xFE, 1, 0}),
            arguments("UTF-32LE", 4, new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 0}),
            arguments(null, 0,       new byte[] {0, 0, (byte) 0xFE}),
            arguments(null, 0,       new byte[] {0, 0, (byte) 0xFE, 0}),
            arguments(null, 0,       new byte[] {0, 0, 0, (byte) 0xFF}),
            arguments(null, 0,       new byte[] {0, 1, (byte) 0xFE, (byte) 0xFF}),
            arguments("UTF-32BE", 4, new byte[] {0, 0, (byte) 0xFE, (byte) 0xFF})
        );
        // @formatter:on
    }

}
