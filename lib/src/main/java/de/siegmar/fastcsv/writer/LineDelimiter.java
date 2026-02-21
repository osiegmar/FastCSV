package de.siegmar.fastcsv.writer;

/// Enumeration for different line delimiters (LF, CR, CRLF, platform default).
public enum LineDelimiter {

    /// Line Feed - (UNIX).
    LF("\n"),

    /// Carriage Return - (Mac classic).
    CR("\r"),

    /// Carriage Return and Line Feed (Windows).
    CRLF("\r\n"),

    /// Use current platform default ([System#lineSeparator()].
    PLATFORM(System.lineSeparator());

    private final String str;

    LineDelimiter(final String str) {
        this.str = str;
    }

    /// Maps a line delimiter string to the corresponding enum constant.
    ///
    /// Accepted values are `"\r\n"` ([#CRLF]), `"\n"` ([#LF]) and `"\r"` ([#CR]).
    /// The [#PLATFORM] constant is never returned by this method.
    ///
    /// @param str the line delimiter string to look up.
    /// @return the matching enum constant.
    /// @throws IllegalArgumentException if the string is not a known line delimiter.
    public static LineDelimiter of(final String str) {
        return switch (str) {
            case "\r\n" -> CRLF;
            case "\n" -> LF;
            case "\r" -> CR;
            default -> throw new IllegalArgumentException("Unknown line delimiter: " + str);
        };
    }

    @Override
    public String toString() {
        return str;
    }

}
