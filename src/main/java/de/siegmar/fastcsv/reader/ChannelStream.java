package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class ChannelStream {

    private final ReadableByteChannel channel;
    private final ByteBuffer byteBuf;
    private final Buffer buf;
    private final StatusConsumer statusConsumer;
    private int totalPosition;
    private int nextByte;

    ChannelStream(final ReadableByteChannel channel, final ByteBuffer byteBuf, final StatusConsumer statusConsumer)
        throws IOException {

        this.channel = channel;

        this.byteBuf = byteBuf;

        // Keep one buf as Buffer to maintain Android compatibility
        // otherwise calls to clear() and flip() cause NoSuchMethodError
        // see https://www.morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/
        this.buf = byteBuf;

        this.statusConsumer = statusConsumer;

        nextByte = loadData() ? byteBuf.get() : -1;
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
        final int readCnt = channel.read(byteBuf);
        buf.flip();
        if (readCnt == -1) {
            return false;
        }
        statusConsumer.addReadBytes(readCnt);
        return true;
    }

    private int fetchNextByte() throws IOException {
        if (!buf.hasRemaining() && !loadData()) {
            return -1;
        }

        totalPosition++;
        return byteBuf.get();
    }

    int getTotalPosition() {
        return totalPosition;
    }

}
