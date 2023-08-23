package de.siegmar.fastcsv.util;

/**
 * Internal utility class.
 * <p>
 * Do <strong>not</strong> part of the API!
 */
public final class Preconditions {

    private Preconditions() {
    }

    /**
     * Checks the given argument and throws an exception if not met.
     *
     * @param expression   the expression that has to be {@code true}
     * @param errorMessage the exception message to be thrown
     * @throws IllegalArgumentException if the {@code expression} is {@code false}.
     */
    public static void checkArgument(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks the given argument and throws an exception if not met.
     *
     * @param expression           the expression that has to be {@code true}
     * @param errorMessageTemplate the exception message template (format {@link String#format(String, Object...)})
     *                             to be thrown
     * @param errorMessageArgs     the exception message arguments
     * @throws IllegalArgumentException if the {@code expression} is {@code false}.
     */
    public static void checkArgument(final boolean expression, final String errorMessageTemplate,
                                     final Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

}
