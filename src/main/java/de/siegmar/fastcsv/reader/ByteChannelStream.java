package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class ByteChannelStream {

    private final ByteBuffer byteBuf = ByteBuffer.allocateDirect(8192);
    private final ReadableByteChannel channel;
    private final StatusListener statusListener;
    private long totalPosition = -1;
    private int nextByte;

    // Keep one buf as Buffer to maintain Android compatibility
    // otherwise calls to clear() and flip() cause NoSuchMethodError
    // see https://www.morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/
    private final Buffer buf = byteBuf;

    ByteChannelStream(final ReadableByteChannel channel, final StatusListener statusListener)
        throws IOException {

        this.channel = channel;
        this.statusListener = statusListener;
        nextByte = loadData() ? byteBuf.get() : -1;
    }

    int get() throws IOException {
        if (nextByte == -1) {
            return -1;
        }

        final int ret = nextByte;
        nextByte = fetchNextByte();
        totalPosition++;
        return ret;
    }

    boolean consumeIfNextEq(final int val) throws IOException {
        if (nextByte != val) {
            return false;
        }

        nextByte = fetchNextByte();
        totalPosition++;
        return true;
    }

    boolean hasData() {
        return nextByte != -1;
    }

    long getTotalPosition() {
        return totalPosition;
    }

    private int fetchNextByte() throws IOException {
        return buf.hasRemaining() || loadData() ? byteBuf.get() : -1;
    }

    private boolean loadData() throws IOException {
        buf.clear();
        final int readCnt = channel.read(byteBuf);
        buf.flip();

        if (readCnt != -1) {
            statusListener.onReadBytes(readCnt);
            return true;
        }

        return false;
    }

}
