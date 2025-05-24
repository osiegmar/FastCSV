package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;

sealed interface CsvParser extends Closeable permits StrictCsvParser {

    /// Parses the next record from the stream and passes it to the callback handler.
    ///
    /// @return `true` if data was parsed, `false` if the end of the stream was reached.
    boolean parse() throws IOException;

    /// {@return the next line from the stream without consuming it.}
    String peekLine() throws IOException;

    /// Skips a line in the stream after skipping the specified number of characters.
    ///
    /// @return `true` if the line was skipped,
    ///     `false` if the end of the stream was reached before any characters could be skipped.
    boolean skipLine(int numCharsToSkip) throws IOException;

    /// {@return the starting line number of the last parsed record.}
    long getStartingLineNumber();

    /// Resets the parser to the specified starting line number.
    void reset(long startingLineNumber);

}
