package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BomInputStreamReaderTest {

    @ParameterizedTest
    @MethodSource
    void charset(final String charset, final byte[] data) throws IOException {
        final var bis = new BomInputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8);

        if (charset == null) {
            assertThat(bis.detectCharset()).isEmpty();
        } else {
            assertThat(bis.detectCharset())
                .get()
                .hasToString(charset);
        }
    }

    static Stream<Arguments> charset() {
        // @formatter:off
        return Stream.of(
            arguments(null,       new byte[] {(byte) 0xEF}),
            arguments(null,       new byte[] {(byte) 0xEF, 0}),
            arguments(null,       new byte[] {(byte) 0xEF, (byte) 0xBB}),
            arguments(null,       new byte[] {(byte) 0xEF, (byte) 0xBB, 0}),
            arguments(null,       new byte[] {(byte) 0xEF, 0, (byte) 0xBF}),
            arguments("UTF-8",    new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}),
            arguments(null,       new byte[] {(byte) 0xFE, 0}),
            arguments("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF}),
            arguments("UTF-16BE", new byte[] {(byte) 0xFE, (byte) 0xFF, 0}),
            arguments(null,       new byte[] {(byte) 0xFF, 0}),
            arguments(null,       new byte[] {0, (byte) 0xFF}),
            arguments("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE, 0}),
            arguments("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 1}),
            arguments("UTF-16LE", new byte[] {(byte) 0xFF, (byte) 0xFE, 1, 0}),
            arguments("UTF-32LE", new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 0}),
            arguments(null,       new byte[] {0, 0, (byte) 0xFE}),
            arguments(null,       new byte[] {0, 0, (byte) 0xFE, 0}),
            arguments(null,       new byte[] {0, 0, 0, (byte) 0xFF}),
            arguments(null,       new byte[] {0, 1, (byte) 0xFE, (byte) 0xFF}),
            arguments("UTF-32BE", new byte[] {0, 0, (byte) 0xFE, (byte) 0xFF})
        );
        // @formatter:on
    }

}
