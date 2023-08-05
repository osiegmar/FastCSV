package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class CsvScanner {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private static final int STATUS_LAST_CHAR_WAS_CR = 32;
    private static final int STATUS_COMMENTED_ROW = 16;
    private static final int STATUS_NEW_FIELD = 8;
    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_COLUMN = 2;
    private static final int STATUS_DATA_COLUMN = 1;
    private static final int STATUS_RESET = 0;

    static void scan(final Path file, final byte quoteCharacter,
                     final StatusConsumer statusConsumer) throws IOException {

        try (ReadableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            scan(quoteCharacter, channel, statusConsumer);
        }
    }

    static void scan(final byte quoteCharacter, final ReadableByteChannel channel,
                     final StatusConsumer statusConsumer) throws IOException {

        final ChannelStream buf = new ChannelStream(channel, ByteBuffer.allocateDirect(8192), statusConsumer);

        int status = STATUS_RESET;

        int d;
        while ((d = buf.get()) != -1) {
            if ((status & STATUS_QUOTED_MODE) != 0) {
                if (d == quoteCharacter) {
                    if (buf.peek() == quoteCharacter) {
                        buf.consume();
                    } else {
                        status &= ~STATUS_QUOTED_MODE;
                    }
                }
            } else {
                if (d == quoteCharacter) {
                    // FIXME quote in data mode?
                    status |= STATUS_QUOTED_MODE;
                } else if (d == CR) {
                    if (buf.peek() == LF) {
                        buf.consume();
                    }

                    if (buf.hasData()) {
                        statusConsumer.addPosition(buf.getTotalPosition());
                    }
                } else if (d == LF) {
                    if (buf.hasData()) {
                        statusConsumer.addPosition(buf.getTotalPosition());
                    }
                }
            }
        }
    }

}
