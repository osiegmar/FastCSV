package de.siegmar.fastcsv;

import java.io.Writer;

import org.openjdk.jmh.infra.Blackhole;

/**
 * Writer implementation that sends all data to a black hole.
 */
class NullWriter extends Writer {

    private final Blackhole blackhole;

    NullWriter(final Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        blackhole.consume(cbuf);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
