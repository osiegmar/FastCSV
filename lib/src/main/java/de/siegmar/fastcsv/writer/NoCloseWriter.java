package de.siegmar.fastcsv.writer;

import java.io.FilterWriter;
import java.io.Writer;

/**
 * A writer that does not close the underlying writer.
 */
class NoCloseWriter extends FilterWriter {

    NoCloseWriter(final Writer out) {
        super(out);
    }

    @Override
    public void close() {
        // do nothing
    }

}
