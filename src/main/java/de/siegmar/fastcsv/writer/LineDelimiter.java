package de.siegmar.fastcsv.writer;

/**
 * Enumeration for different line delimiters (LF, CR, CRLF, platform default).
 */
public enum LineDelimiter {

    /**
     * Line Feed - (UNIX).
     */
    LF,

    /**
     * Carriage Return - (Mac classic).
     */
    CR,

    /**
     * Carriage Return and Line Feed (Windows).
     */
    CRLF,

    /**
     * Use current platform default ({@link System#lineSeparator()}.
     */
    PLATFORM;

    /**
     * Build an enum based on the given string.
     *
     * @param str the string to convert to an enum.
     * @return the enum representation of the given string.
     */
    public static LineDelimiter of(final String str) {
        if ("\r\n".equals(str)) {
            return CRLF;
        }
        if ("\n".equals(str)) {
            return LF;
        }
        if ("\r".equals(str)) {
            return CR;
        }
        throw new IllegalArgumentException("Unknown line delimiter: " + str);
    }

    @SuppressWarnings("checkstyle:returncount")
    @Override
    public String toString() {
        switch (this) {
            case CRLF:
                return "\r\n";
            case LF:
                return "\n";
            case CR:
                return "\r";
            case PLATFORM:
                return System.lineSeparator();
            default:
                throw new IllegalStateException();
        }
    }

}
