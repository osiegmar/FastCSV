package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Consumer;

final class CsvScanner {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final byte commentCharacter;
    private final Consumer<Long> positionConsumer;
    private final StatusListener statusListener;
    private final ChannelStream buf;

    CsvScanner(final ReadableByteChannel channel, final byte fieldSeparator, final byte quoteCharacter,
               final CommentStrategy commentStrategy, final byte commentCharacter,
               final Consumer<Long> positionConsumer, final StatusListener statusListener) throws IOException {

        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.positionConsumer = positionConsumer;
        this.statusListener = statusListener;

        buf = new ChannelStream(channel, statusListener);
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "checkstyle:CyclomaticComplexity",
        "checkstyle:NestedIfDepth"})
    void scan() throws IOException {
        if (buf.peek() != -1) {
            addRowPosition(0);
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

    @SuppressWarnings("PMD.AssignmentInOperand")
    private void consumeCommentedRow() throws IOException {
        int d;
        while ((d = buf.get()) != -1) {
            if (d == CR) {
                if (buf.peek() == LF) {
                    buf.consume();
                }

                if (buf.hasData()) {
                    addRowPosition(buf.getTotalPosition());
                }
                break;
            } else if (d == LF) {
                if (buf.hasData()) {
                    addRowPosition(buf.getTotalPosition());
                }
                break;
            }
        }
    }

    private void addRowPosition(final long totalPosition) {
        positionConsumer.accept(totalPosition);
        statusListener.readRow();
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
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

    @SuppressWarnings({"PMD.AvoidReassigningParameters", "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment", "checkstyle:ReturnCount"})
    private boolean consumeUnquotedField(int d) throws IOException {
        do {
            if (d == fieldSeparator) {
                return false;
            } else if (d == CR) {
                if (buf.peek() == LF) {
                    buf.consume();
                }

                if (buf.hasData()) {
                    addRowPosition(buf.getTotalPosition());
                }
                return true;
            } else if (d == LF) {
                if (buf.hasData()) {
                    addRowPosition(buf.getTotalPosition());
                }
                return true;
            }
        } while ((d = buf.get()) != -1);

        return true;
    }

}
