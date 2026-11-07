package de.siegmar.fastcsv.util;

import java.util.HexFormat;

/// Internal utility class.
///
/// It is **not** a part of the API!
public final class Util {

    /// Carriage return.
    public static final char CR = '\r';

    /// Line feed.
    public static final char LF = '\n';

    private static final HexFormat HEX = HexFormat.of();

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

    /// Checks if the given character is a line break character.
    ///
    /// @param character character to test
    /// @return `true` if the argument is [#CR] or [#LF], `false` otherwise
    public static boolean isNewline(final char character) {
        return character == CR || character == LF;
    }

    /// Checks if the given string contains any line break characters.
    ///
    /// @param str the string to check for line break characters
    /// @return `true` if the string contains either [#CR] or [#LF], `false` otherwise
    public static boolean containsNewline(final String str) {
        return str.indexOf(CR) >= 0 || str.indexOf(LF) >= 0;
    }

    /// Replaces every character that is not a letter, mark, number, punctuation, symbol or space with its Unicode
    /// escapes: control and format characters, line and paragraph separators, unpaired surrogates, private-use and
    /// unassigned characters.
    ///
    /// Exception messages that contain text from the CSV data use this, so that the text cannot fake a line in a
    /// log file, reorder the text on display with bidi control characters or send terminal commands. Characters the
    /// running Java version does not know yet are escaped as well.
    ///
    /// @param str the string to escape
    /// @return the escaped string, never `null`
    public static String escapeControlCharacters(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());
        str.codePoints().forEach(codePoint -> {
            if (needsEscape(codePoint)) {
                for (final char ch : Character.toChars(codePoint)) {
                    sb.append("\\u").append(HEX.toHexDigits(ch));
                }
            } else {
                sb.appendCodePoint(codePoint);
            }
        });
        return sb.toString();
    }

    private static boolean needsEscape(final int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.PRIVATE_USE,
                 Character.UNASSIGNED, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true;
            default -> false;
        };
    }

}
