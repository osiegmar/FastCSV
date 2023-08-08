package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class ChannelStream {

    private final ReadableByteChannel channel;
    private final ByteBuffer buf;
    private final StatusConsumer statusConsumer;
    private int totalPosition;
    private int nextByte;

    ChannelStream(final ReadableByteChannel channel, final ByteBuffer buf, final StatusConsumer statusConsumer)
        throws IOException {

        this.channel = channel;
        this.buf = buf;
        this.statusConsumer = statusConsumer;

        nextByte = loadData(false) ? buf.get() : -1;
    }

    int peek() {
        return nextByte;
    }

    int get() throws IOException {
        final int ret = nextByte;
        nextByte = fetchNextByte();
        return ret;
    }

    void consume() throws IOException {
        nextByte = fetchNextByte();
    }

    boolean hasData() {
        return nextByte != -1;
    }

    private boolean loadData(final boolean compact) throws IOException {
        final int readCnt = channel.read(compact ? buf.compact() : buf);
        if (readCnt == -1) {
            return false;
        }
        buf.flip();
        statusConsumer.addReadBytes(readCnt);
        return true;
    }

    private int fetchNextByte() throws IOException {
        if (!buf.hasRemaining() && !loadData(true)) {
            return -1;
        }

        totalPosition++;
        return buf.get();
    }

    int getTotalPosition() {
        return totalPosition;
    }

}
