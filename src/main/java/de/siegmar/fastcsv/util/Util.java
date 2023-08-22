package de.siegmar.fastcsv.util;

/**
 * Internal utility class.
 * <p>
 * Do <strong>not</strong> part of the API!
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
     * Checks if at least one of the passed arguments appear more than once.
     *
     * @param chars characters to check for uniqueness
     * @return {@code true} if at least one of the passed arguments appear more than once
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

}
