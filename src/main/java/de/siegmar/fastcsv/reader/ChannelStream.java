package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class ChannelStream {

    private final ReadableByteChannel channel;
    private final ByteBuffer buf;
    private final StatusConsumer statusConsumer;
    private int totalPosition;

    ChannelStream(final ReadableByteChannel channel, final ByteBuffer buf, final StatusConsumer statusConsumer)
        throws IOException {

        this.channel = channel;
        this.buf = buf;
        this.statusConsumer = statusConsumer;

        final int readCnt = channel.read(buf);
        buf.flip();
        if (readCnt != -1) {
            statusConsumer.addReadBytes(readCnt);
        }
    }

    int get() throws IOException {
        if (!buf.hasRemaining()) {
            if (getReadCnt() == -1) {
                return -1;
            }
        }

        totalPosition++;

        return buf.get();
    }

    private int getReadCnt() throws IOException {
        final int readCnt = channel.read(buf.compact());
        buf.flip();
        if (readCnt != -1) {
            statusConsumer.addReadBytes(readCnt);
        }
        return readCnt;
    }

    int peek() throws IOException {
        if (!buf.hasRemaining()) {
            if (getReadCnt() == -1) {
                return -1;
            }
        }

        return buf.get(buf.position());
    }

    int getTotalPosition() {
        return totalPosition;
    }

    boolean hasData() throws IOException {
        if (!buf.hasRemaining()) {
            final int readCnt = getReadCnt();
            if (readCnt == -1) {
                return false;
            }
        }

        return buf.hasRemaining();
    }

    void consume() {
        totalPosition++;
        buf.get();
    }

}
