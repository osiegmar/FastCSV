package de.siegmar.fastcsv.reader;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.junit.jupiter.api.Test;

class ChannelStreamTest {

    @Test
    void foo() throws IOException {
        final ByteBuffer allocate = ByteBuffer.allocate(1);
        final ChannelStream channelStream = new ChannelStream(channelOf(new byte[]{1, 2}), allocate, new StatusConsumer() {
            @Override
            public void addPosition(final int position) {

            }

            @Override
            public void addReadBytes(final int readCnt) {

            }
        });

        assertThat(channelStream.get()).isEqualTo(1);

        assertThat(channelStream.peek()).isEqualTo(2);
        assertThat(channelStream.get()).isEqualTo(2);

        assertThat(channelStream.peek()).isEqualTo(-1);
        assertThat(channelStream.get()).isEqualTo(-1);
    }

    private static ReadableByteChannel channelOf(final byte[] buf) {
        return Channels.newChannel(new ByteArrayInputStream(buf));
    }

}
