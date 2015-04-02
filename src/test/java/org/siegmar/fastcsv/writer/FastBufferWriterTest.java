package org.siegmar.fastcsv.writer;

import java.io.IOException;
import java.io.StringWriter;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class FastBufferWriterTest {

    private FastBufferedWriter fbw;
    private StringWriter sw;

    @BeforeMethod
    public void init() {
        sw = new StringWriter();
        fbw = new FastBufferedWriter(sw);
    }

    public void appendSingle() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            fbw.append('a');
            fbw.append('b');
        }
        fbw.close();

        assertEquals(sw.toString(), sb.toString());
    }

    public void appendArray() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            fbw.append("ab");
        }
        fbw.close();

        assertEquals(sw.toString(), sb.toString());
    }

    public void appendLarge() throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
        }
        fbw.append(sb.toString());

        // also test flush
        fbw.flush();

        assertEquals(sw.toString(), sb.toString());
    }

}
