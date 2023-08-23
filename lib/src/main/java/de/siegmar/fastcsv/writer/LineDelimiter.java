package de.siegmar.fastcsv.writer;

/**
 * Enumeration for different line delimiters (LF, CR, CRLF, platform default).
 */
public enum LineDelimiter {

    /**
     * Line Feed - (UNIX).
     */
    LF("\n"),

    /**
     * Carriage Return - (Mac classic).
     */
    CR("\r"),

    /**
     * Carriage Return and Line Feed (Windows).
     */
    CRLF("\r\n"),

    /**
     * Use current platform default ({@link System#lineSeparator()}.
     */
    PLATFORM(System.lineSeparator());

    private final String str;

    LineDelimiter(final String str) {
        this.str = str;
    }

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

    @Override
    public String toString() {
        return str;
    }

}
