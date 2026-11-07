package de.siegmar.fastcsv.reader;

import java.io.Serial;

/// Exception thrown when malformed CSV data is read or a limit is exceeded.
/// It also wraps any other exception thrown while reading records, for example by a [FieldModifier], except for
/// I/O errors, which are reported as [java.io.UncheckedIOException] or [java.io.IOException].
public class CsvParseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /// Constructs an exception with a message.
    ///
    /// @param message the detail message
    public CsvParseException(final String message) {
        super(message);
    }

    /// Constructs an exception with a message and a cause.
    ///
    /// @param message the detail message
    /// @param cause the cause for this exception
    public CsvParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
