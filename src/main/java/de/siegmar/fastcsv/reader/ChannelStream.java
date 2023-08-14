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

        nextByte = loadData() ? buf.get() : -1;
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

    private boolean loadData() throws IOException {
        buf.clear();
        final int readCnt = channel.read(buf);
        if (readCnt == -1) {
            return false;
        }
        statusConsumer.addReadBytes(readCnt);
        buf.flip();
        return true;
    }

    private int fetchNextByte() throws IOException {
        if (!buf.hasRemaining() && !loadData()) {
            return -1;
        }

        totalPosition++;
        return buf.get();
    }

    int getTotalPosition() {
        return totalPosition;
    }

}
