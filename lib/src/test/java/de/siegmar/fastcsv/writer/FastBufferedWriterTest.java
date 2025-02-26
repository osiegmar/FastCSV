package de.siegmar.fastcsv.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class FastBufferedWriterTest {

    private final StringWriter sw = new StringWriter();
    private final FastBufferedWriter cw = new FastBufferedWriter(sw, 8192, false, false);

    @Test
    void appendSingle() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write('a');
            cw.write('b');
        }
        cw.close();

        assertThat(sb).asString().isEqualTo(sw.toString());
    }

    @Test
    void appendArray() throws IOException {
        final StringBuilder sb = new StringBuilder();

        final String strValue = "ab";
        final char[] caValue = strValue.toCharArray();
        for (int i = 0; i < 8192; i++) {
            sb.append(strValue);
            cw.write(caValue, 0, caValue.length);
        }
        cw.close();

        assertThat(sb).asString().isEqualTo(sw.toString());
    }

    @Test
    void appendString() throws IOException {
        final StringBuilder sb = new StringBuilder();

        final String strValue = "ab";
        for (int i = 0; i < 8192; i++) {
            sb.append(strValue);
            cw.write(strValue, 0, 2);
        }
        cw.close();

        assertThat(sb).asString().isEqualTo(sw.toString());
    }

    @Test
    void appendLargeString() throws IOException {
        final String sb = buildLargeData();
        cw.write(sb, 0, sb.length());

        assertThat(sw).asString().isEqualTo(sb);
    }

    @Test
    void appendLargeArray() throws IOException {
        final char[] data = buildLargeData().toCharArray();
        cw.write(data, 0, data.length);

        assertThat(sw).asString().isEqualTo(new String(data));
    }

    @Test
    void autoFlushBuffer() throws IOException {
        final var stringWriter = new StringWriter() {
            @Override
            public void flush() {
                throw new UnsupportedOperationException();
            }
        };

        final var fbw = new FastBufferedWriter(stringWriter, 8, true, false);

        fbw.write("foo");
        assertThat(stringWriter).asString().isEmpty();

        fbw.endRecord();
        assertThat(stringWriter).asString().isEqualTo("foo");
    }

    @Test
    void autoFlushWriter() throws IOException {
        final AtomicInteger flushCount = new AtomicInteger();
        final var stringWriter = new StringWriter() {
            @Override
            public void flush() {
                flushCount.incrementAndGet();
            }
        };

        final var fbw = new FastBufferedWriter(stringWriter, 8, true, true);

        fbw.write("bar");
        assertThat(stringWriter).asString().isEmpty();

        fbw.endRecord();
        assertThat(stringWriter).asString().isEqualTo("bar");
        assertThat(flushCount).hasValue(1);
    }

    private String buildLargeData() {
        return "ab".repeat(8192);
    }

}
