package de.siegmar.fastcsv.writer;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/// Implementation of [Writable] that does not buffer any data
/// but flushes the underlying writer at the end of each record if configured.
final class UnbufferedWriter extends FilterWriter implements Writable {

    private final boolean autoFlushWriter;

    UnbufferedWriter(final Writer out, final boolean autoFlushWriter) {
        super(out);
        this.autoFlushWriter = autoFlushWriter;
    }

    @Override
    public void endRecord() throws IOException {
        if (autoFlushWriter) {
            flush();
        }
    }

}
