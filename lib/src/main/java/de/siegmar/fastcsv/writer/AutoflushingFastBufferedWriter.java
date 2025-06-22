package de.siegmar.fastcsv.writer;

import java.io.IOException;
import java.io.Writer;

final class AutoflushingFastBufferedWriter extends FastBufferedWriter {

    AutoflushingFastBufferedWriter(final Writer writer, final int bufferSize) {
        super(writer, bufferSize);
    }

    @Override
    public void endRecord() throws IOException {
        flush();
    }

}
