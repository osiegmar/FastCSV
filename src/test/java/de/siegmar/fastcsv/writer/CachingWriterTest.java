package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class CachingWriterTest {

    private final StringWriter sw = new StringWriter();
    private final CachingWriter cw = new CachingWriter(sw);

    @Test
    public void appendSingle() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write('a');
            cw.write('b');
        }
        cw.close();

        assertEquals(sb.toString(), sw.toString());
    }

    @Test
    public void appendArray() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write("ab", 0, 2);
        }
        cw.close();

        assertEquals(sb.toString(), sw.toString());
    }

    @Test
    public void appendLarge() throws IOException {
        final String sb = buildLargeData();
        cw.write(sb, 0, sb.length());

        assertEquals(sb, sw.toString());
    }

    private String buildLargeData() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
        }
        return sb.toString();
    }

}
