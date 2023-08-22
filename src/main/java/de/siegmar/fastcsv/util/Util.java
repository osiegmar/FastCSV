package de.siegmar.fastcsv.util;

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
