package de.siegmar.fastcsv.util;

/**
 * Internal utility class used to uncheck checked exceptions.
 */
public final class Unchecker {

    private Unchecker() {
    }

    /**
     * Re-throw the given exception unchecked.
     *
     * @param e the exception to re-throw.
     */
    public static void uncheck(final Exception e) {
        Unchecker.hide(e);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void hide(final Exception e) throws T {
        throw (T) e;
    }

}
