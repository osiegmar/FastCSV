package de.siegmar.fastcsv;

import java.io.Writer;
import java.util.Arrays;

import org.openjdk.jmh.infra.Blackhole;

/// Writer implementation that sends all data to a black hole.
public class NullWriter extends Writer {

    private final Blackhole bh;

    /// Initializes a new instance of the [NullWriter] class.
    ///
    /// @param bh the black hole to send all data to
    public NullWriter(final Blackhole bh) {
        this.bh = bh;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        bh.consume(Arrays.copyOfRange(cbuf, off, off + len));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
