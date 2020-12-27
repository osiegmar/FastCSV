package de.siegmar.fastcsv.reader;

/**
 * Exception to be thrown when malformed csv data is read.
 */
public class MalformedCsvException extends RuntimeException {

    static final long serialVersionUID = 1L;

    /**
     * Construct exception with message.
     *
     * @param message the cause for this exception
     */
    public MalformedCsvException(final String message) {
        super(message);
    }

}
