package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class ChannelStream {

    private final ByteBuffer byteBuf = ByteBuffer.allocateDirect(8192);
    private final ReadableByteChannel channel;
    private final StatusListener statusListener;
    private long totalPosition;
    private int nextByte;

    // Keep one buf as Buffer to maintain Android compatibility
    // otherwise calls to clear() and flip() cause NoSuchMethodError
    // see https://www.morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/
    private final Buffer buf = byteBuf;

    ChannelStream(final ReadableByteChannel channel, final StatusListener statusListener)
        throws IOException {

        this.channel = channel;
        this.statusListener = statusListener;
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
        statusListener.readBytes(readCnt);
        return true;
    }

    private int fetchNextByte() throws IOException {
        if (!buf.hasRemaining() && !loadData()) {
            return -1;
        }

        totalPosition++;
        return byteBuf.get();
    }

    long getTotalPosition() {
        return totalPosition;
    }

}
