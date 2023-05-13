package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class FastBufferedWriterTest {

    private final StringWriter sw = new StringWriter();
    private final CsvWriter.FastBufferedWriter cw = new CsvWriter.FastBufferedWriter(sw, 8192);

    @Test
    void appendSingle() throws IOException {
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
    void appendArray() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write("ab", 0, 2);
        }
        cw.close();

        assertEquals(sb.toString(), sw.toString());
    }

    @Test
    void appendLarge() throws IOException {
        final String sb = buildLargeData();
        cw.write(sb, 0, sb.length());

        assertEquals(sb, sw.toString());
    }

    @Test
    void unreachable() {
        assertThrows(IllegalStateException.class, () -> cw.write(new char[0], 0, 0));
    }

    private String buildLargeData() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
        }
        return sb.toString();
    }

}
