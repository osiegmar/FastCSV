package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Consumer;

final class CsvScanner {

    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final byte commentCharacter;
    private final Consumer<Long> positionConsumer;
    private final StatusListener statusListener;
    private final ByteChannelStream stream;
    private final boolean ignoreComments;

    CsvScanner(final ReadableByteChannel channel, final byte fieldSeparator, final byte quoteCharacter,
               final CommentStrategy commentStrategy, final byte commentCharacter,
               final Consumer<Long> positionConsumer, final StatusListener statusListener) throws IOException {

        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.positionConsumer = positionConsumer;
        this.statusListener = statusListener;

        ignoreComments = commentStrategy == CommentStrategy.NONE;

        stream = new ByteChannelStream(channel, statusListener);
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "checkstyle:CyclomaticComplexity",
        "checkstyle:NestedIfDepth"})
    void scan() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            positionConsumer.accept(stream.getTotalPosition());

            // parse a row
            if (d != commentCharacter || ignoreComments) {
                do {
                    // parse fields
                    if (d == quoteCharacter) {
                        if (consumeQuotedField()) {
                            // reached end of line
                            break;
                        }
                    } else if (consumeUnquotedField(d)) {
                        // reached end of line
                        break;
                    }
                } while ((d = stream.get()) != -1);
            } else {
                consumeCommentedRow();
            }

            statusListener.onReadRow();
        }
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private boolean consumeQuotedField() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            if (d == quoteCharacter) {
                if (!stream.consumeIfNextEq(quoteCharacter)) {
                    break;
                }
            }
        }

        if (stream.hasData()) {
            // handle all kinds of characters after closing quote
            return consumeUnquotedField(stream.get());
        }

        return false;
    }

    @SuppressWarnings({"PMD.AvoidReassigningParameters", "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment"})
    private boolean consumeUnquotedField(int d) throws IOException {
        do {
            if (d == fieldSeparator) {
                return false;
            } else if (d == CR) {
                stream.consumeIfNextEq(LF);
                break;
            } else if (d == LF) {
                break;
            }
        } while ((d = stream.get()) != -1);

        return true;
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private void consumeCommentedRow() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            if (d == CR) {
                stream.consumeIfNextEq(LF);
                break;
            } else if (d == LF) {
                break;
            }
        }
    }

}
