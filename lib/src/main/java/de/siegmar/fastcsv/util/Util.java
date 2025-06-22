package de.siegmar.fastcsv.util;

/// Internal utility class.
///
/// It is **not** a part of the API!
public final class Util {

    /// Carriage return.
    public static final char CR = '\r';

    /// Line feed.
    public static final char LF = '\n';

    private Util() {
    }

    /// Checks if the given array of characters contains any duplicate characters.
    ///
    /// @param chars the array of characters to check for duplicates
    /// @return `true` if any character appears more than once in the array, `false` otherwise
    public static boolean containsDupe(final char... chars) {
        for (int i = 0; i < chars.length; i++) {
            for (int j = i + 1; j < chars.length; j++) {
                if (chars[i] == chars[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Checks if the given character is a newline character.
    ///
    /// @param character character to test.
    /// @return `true` if the argument is [#CR] or [#LF]
    public static boolean isNewline(final char character) {
        return character == CR || character == LF;
    }

    /// Checks if the given string contains any newline characters.
    ///
    /// @param str the string to check for newlines
    /// @return `true` if the string contains either [#CR] or [#LF], `false` otherwise
    public static boolean containsNewline(final String str) {
        return str.indexOf(CR) >= 0 || str.indexOf(LF) >= 0;
    }

}
