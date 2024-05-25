package de.siegmar.fastcsv.util;

/**
 * Internal utility class.
 * <p>
 * It is <strong>not</strong> a part of the API!
 */
public final class Util {

    /**
     * Carriage return.
     */
    public static final char CR = '\r';

    /**
     * Line feed.
     */
    public static final char LF = '\n';

    private Util() {
    }

    /**
     * Checks if the given array of characters contains any duplicate characters.
     *
     * @param chars the array of characters to check for duplicates
     * @return {@code true} if any character appears more than once in the array, {@code false} otherwise
     */
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

    /**
     * Checks if the given character is a newline character.
     *
     * @param character character to test.
     * @return {@code true} if the argument is {@value CR} or {@value LF}
     */
    public static boolean isNewline(final char character) {
        return character == CR || character == LF;
    }

}
