package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.CR;
import static de.siegmar.fastcsv.util.Util.LF;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

final class CsvScanner {

    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final byte commentCharacter;
    private final CsvListener csvListener;
    private final ByteChannelStream stream;
    private final boolean readComments;

    CsvScanner(final ReadableByteChannel channel, final int bomHeaderLength, final byte fieldSeparator,
               final byte quoteCharacter, final CommentStrategy commentStrategy, final byte commentCharacter,
               final CsvListener csvListener) throws IOException {

        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentCharacter = commentCharacter;
        this.csvListener = csvListener;

        readComments = commentStrategy != CommentStrategy.NONE;

        stream = new ByteChannelStream(channel, csvListener);

        if (bomHeaderLength > 0) {
            for (int i = 0; i < bomHeaderLength; i++) {
                stream.get();
            }
        }
    }

    @SuppressWarnings({"PMD.AssignmentInOperand", "checkstyle:CyclomaticComplexity",
        "checkstyle:NestedIfDepth"})
    void scan() throws IOException {
        int d;
        while ((d = stream.get()) != -1) {
            csvListener.startOffset(stream.getOffset());

            // parse a record
            if (d == commentCharacter && readComments) {
                consumeCommentedLine();
            } else {
                consumeRecord(d);
            }

            csvListener.onReadRecord();
        }
    }

    @SuppressWarnings({"PMD.AvoidReassigningParameters", "checkstyle:FinalParameters",
        "checkstyle:ParameterAssignment"})
    private void consumeRecord(int d) throws IOException {
        do {
            // parse fields
            if (d == quoteCharacter) {
                if (consumeQuotedField()) {
                    // reached the end of record
                    break;
                }
            } else if (consumeUnquotedField(d)) {
                // reached the end of record
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
                csvListener.additionalLine();
            } else if (d == LF) {
                csvListener.additionalLine();
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

    public interface CsvListener {

        void onReadBytes(int readCnt);

        void startOffset(long offset);

        void onReadRecord();

        void additionalLine();

    }

}
