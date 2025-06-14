package de.siegmar.fastcsv.util;

import java.util.function.Supplier;

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
    /// @param errorMessageSupplier a supplier for the exception message to be thrown
    /// @throws IllegalArgumentException if the `expression` is `false`.
    @SuppressWarnings("AnnotateFormatMethod")
    public static void checkArgument(final boolean expression, final Supplier<String> errorMessageSupplier) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessageSupplier.get());
        }
    }

}
