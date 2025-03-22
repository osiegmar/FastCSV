package de.siegmar.fastcsv.util;

/// Internal utility class.
///
/// It is **not** a part of the API!
public final class Preconditions {

    private Preconditions() {
    }

    /// Checks the given argument and throws an exception if not met.
    ///
    /// @param expression   the expression that has to be `true`
    /// @param errorMessage the exception message to be thrown
    /// @throws IllegalArgumentException if the `expression` is `false`.
    public static void checkArgument(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /// Checks the given argument and throws an exception if not met.
    ///
    /// @param expression           the expression that has to be `true`
    /// @param errorMessageTemplate the exception message template (format [String#format(String, Object...)])
    ///                             to be thrown
    /// @param errorMessageArgs     the exception message arguments
    /// @throws IllegalArgumentException if the `expression` is `false`.
    public static void checkArgument(final boolean expression, final String errorMessageTemplate,
                                     final Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessageTemplate.formatted(errorMessageArgs));
        }
    }

}
