package de.siegmar.fastcsv.reader;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvScannerTest {

//    @Test
//    void nullInput() {
//        assertThrows(NullPointerException.class, () -> builder.scan((InputStream) null));
//    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void emptyInput(final String str) throws IOException {
        assertThat(scan(str)).isEmpty();
    }

    @Test
    void oneLine() throws IOException {
        assertThat(scan("012")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"012␊", "012␍", "012␍␊"})
    void oneLineTerminated(final String str) throws IOException {
        assertThat(scan(str)).isEmpty();
    }

    @Test
    void oneLineDoubleTerminated() throws IOException {
        assertThat(scan("012␊␊5")).containsExactly(4, 5);
    }

    @Test
    void lf() throws IOException {
        assertThat(scan("012␊456")).containsExactly(4);
    }

    @Test
    void cr() throws IOException {
        assertThat(scan("012␍456")).containsExactly(4);
    }

    @Test
    void crlf() throws IOException {
        assertThat(scan("012␍␊567")).containsExactly(5);
    }

    @Test
    void quoted() throws IOException {
        assertThat(scan("'123␊5'␊8")).containsExactly(8);
    }

    @Test
    void escapedQuote() throws IOException {
        assertThat(scan("'1''␊5'␊8")).containsExactly(8);
    }

    @Test
    void multipleLines() throws IOException {
        assertThat(scan("012␊456␍890␍␊123"))
            .containsExactly(4, 8, 13)
            .inOrder();
    }

    @ParameterizedTest
    @ValueSource(ints = {8190, 8191, 8192, 8193})
    void bufferExceed(final int pos) throws IOException {
        final byte[] buf = new byte[pos + 2];
        Arrays.fill(buf, (byte) 'A');

        assertThat(scan(buf)).isEmpty();

        buf[pos] = '\n';
        assertThat(scan(buf)).containsExactly(pos + 1);
    }

    // TODO binary data test

    private List<Integer> scan(final String s) throws IOException {
        final byte[] data = s
            .replaceAll("␊", "\n")
            .replaceAll("␍", "\r")
            .replaceAll("'", "\"")
            .getBytes(StandardCharsets.UTF_8);
        return scan(data);
    }

    private List<Integer> scan(final byte[] data) throws IOException {
        final List<Integer> positions = new ArrayList<>();

        final StatusConsumer statusConsumer = new StatusConsumer() {
            @Override
            public void addPosition(final int position) {
                positions.add(position);
            }

            @Override
            public void addReadBytes(final int readCnt) {

            }
        };

        CsvScanner.scan((byte) '"', Channels.newChannel(new ByteArrayInputStream(data)), statusConsumer);

        return positions;
    }

}
