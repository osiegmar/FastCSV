package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

final class CsvScanner {

    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final byte commentCharacter;
    private final Listener listener;
    private final ByteChannelStream stream;
    private final boolean readComments;

    CsvScanner(final ReadableByteChannel channel, final byte fieldSeparator, final byte quoteCharacter,
               final CommentStrategy commentStrategy, final byte commentCharacter,
               final Listener listener) throws IOException {

        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.listener = listener;

        readComments = commentStrategy != CommentStrategy.NONE;

        stream = new ByteChannelStream(channel, listener);
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "checkstyle:CyclomaticComplexity",
        "checkstyle:NestedIfDepth"})
    void scan() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            listener.startOffset(stream.getTotalOffset());

            // parse a row
            if (d == commentCharacter && readComments) {
                consumeCommentedLine();
            } else {
                consumeRow(d);
            }

            listener.onReadRow();
        }
    }

    @SuppressWarnings({"PMD.AvoidReassigningParameters", "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment"})
    private void consumeRow(int d) throws IOException {
        do {
            // parse fields
            if (d == quoteCharacter) {
                if (consumeQuotedField()) {
                    // reached end of row
                    break;
                }
            } else if (consumeUnquotedField(d)) {
                // reached end of row
                break;
            }
        } while ((d = stream.get()) != -1);
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private boolean consumeQuotedField() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            if (d == quoteCharacter) {
                if (!stream.consumeIfNextEq(quoteCharacter)) {
                    break;
                }
            } else if (d == CR) {
                stream.consumeIfNextEq(LF);
                listener.additionalLine();
            } else if (d == LF) {
                listener.additionalLine();
            }
        }

        // handle all kinds of characters after closing quote
        return stream.hasData() && consumeUnquotedField(stream.get());
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
    private void consumeCommentedLine() throws IOException {
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

    public interface Listener {

        default void onReadBytes(int readCnt) {
        }

        default void startOffset(long offset) {
        }

        default void onReadRow() {
        }

        default void additionalLine() {
        }

    }

}
