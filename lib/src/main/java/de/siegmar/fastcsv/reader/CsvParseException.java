package de.siegmar.fastcsv.reader;

/**
 * Exception to be thrown when malformed csv data is read.
 */
public class CsvParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct exception with message.
     *
     * @param message the cause for this exception
     */
    public CsvParseException(final String message) {
        super(message);
    }

    /**
     * Construct exception with message and cause.
     *
     * @param message the cause for this exception
     * @param cause the cause for this exception
     */
    public CsvParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
