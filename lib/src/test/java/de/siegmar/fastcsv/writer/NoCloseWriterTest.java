package de.siegmar.fastcsv.writer;

import static org.assertj.core.api.Assertions.fail;

import java.io.FilterWriter;

import org.junit.jupiter.api.Test;

class NoCloseWriterTest {

    @Test
    void noClose() {
        final var closeFailWriter = new FilterWriter(FilterWriter.nullWriter()) {
            @Override
            public void close() {
                fail();
            }
        };

        new NoCloseWriter(closeFailWriter).close();
    }

}
