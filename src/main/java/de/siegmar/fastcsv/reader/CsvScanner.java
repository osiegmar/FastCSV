package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class CsvScanner {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final byte commentCharacter;
    private final StatusConsumer statusConsumer;
    private final ChannelStream buf;

    CsvScanner(final ReadableByteChannel channel, final byte fieldSeparator, final byte quoteCharacter,
               final CommentStrategy commentStrategy, final byte commentCharacter,
               final StatusConsumer statusConsumer) throws IOException {

        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.statusConsumer = statusConsumer;

        buf = new ChannelStream(channel, ByteBuffer.allocateDirect(8192), statusConsumer);
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "checkstyle:CyclomaticComplexity",
        "checkstyle:NestedIfDepth"})
    void scan() throws IOException {
        if (buf.peek() != -1) {
            statusConsumer.addRecordPosition(0);
        }

        int d;
        while ((d = buf.get()) != -1) {
            // here: always a new row
            if (d == commentCharacter && commentStrategy != CommentStrategy.NONE) {
                consumeCommentedRow();
            } else {
                do {
                    // here: always a new field
                    final boolean endOfRow = (d == quoteCharacter)
                        ? consumeQuotedField() : consumeUnquotedField(d);

                    if (endOfRow) {
                        break;
                    }
                } while ((d = buf.get()) != -1);
            }
        }
    }

    private void consumeCommentedRow() throws IOException {
        int d;
        while ((d = buf.get()) != -1) {
            if (d == CR) {
                if (buf.peek() == LF) {
                    buf.consume();
                }

                if (buf.hasData()) {
                    statusConsumer.addRecordPosition(buf.getTotalPosition());
                }
                break;
            } else if (d == LF) {
                if (buf.hasData()) {
                    statusConsumer.addRecordPosition(buf.getTotalPosition());
                }
                break;
            }
        }
    }

    private boolean consumeQuotedField() throws IOException {
        int d;
        while ((d = buf.get()) != -1) {
            if (d == quoteCharacter) {
                if (buf.peek() == quoteCharacter) {
                    buf.consume();
                } else {
                    return !buf.hasData() || consumeUnquotedField(buf.get());
                }
            }
        }

        return true;
    }

    private boolean consumeUnquotedField(int d) throws IOException {
        do {
            if (d == fieldSeparator) {
                return false;
            } else if (d == CR) {
                if (buf.peek() == LF) {
                    buf.consume();
                }

                if (buf.hasData()) {
                    statusConsumer.addRecordPosition(buf.getTotalPosition());
                }
                return true;
            } else if (d == LF) {
                if (buf.hasData()) {
                    statusConsumer.addRecordPosition(buf.getTotalPosition());
                }
                return true;
            }
        } while ((d = buf.get()) != -1);

        return true;
    }

}
