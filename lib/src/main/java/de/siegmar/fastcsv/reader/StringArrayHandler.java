package de.siegmar.fastcsv.reader;

import java.util.Objects;
import java.util.function.Consumer;

/// A [CsvCallbackHandler] implementation that returns the fields of each record as an array of Strings.
///
/// Example:
/// ```
/// StringArrayHandler handler = StringArrayHandler.builder()
///     .fieldModifier(FieldModifiers.TRIM)
///     .build();
/// ```
///
/// This implementation is stateful and must not be reused.
public final class StringArrayHandler extends AbstractInternalCsvCallbackHandler<String[]> {

    /// Constructs a new `StringArrayHandler`.
    ///
    /// @deprecated Use [#of()] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public StringArrayHandler() {
        super();
    }

    /// Constructs a new `StringArrayHandler` with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public StringArrayHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    private StringArrayHandler(final int maxFields, final int maxFieldSize, final int maxRecordSize,
                               final FieldModifier fieldModifier) {
        super(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
    }

    /// Constructs a new builder instance for this class.
    ///
    /// @return the builder
    /// @see #of(Consumer)
    public static StringArrayHandlerBuilder builder() {
        return new StringArrayHandlerBuilder();
    }

    /// Constructs a new instance of this class with default settings.
    ///
    /// @return the new instance
    /// @see StringArrayHandlerBuilder#build()
    public static StringArrayHandler of() {
        return builder().build();
    }

    /// Constructs a new instance of this class with the given configuration.
    ///
    /// This is an alternative to the builder pattern for convenience.
    ///
    /// @param configurer the configuration, must not be `null`
    /// @return the new instance
    /// @throws NullPointerException if `null` is passed
    /// @throws IllegalArgumentException if argument constraints are violated
    /// @see #builder()
    public static StringArrayHandler of(final Consumer<StringArrayHandlerBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        final StringArrayHandlerBuilder builder = builder();
        configurer.accept(builder);
        return builder.build();
    }

    @Override
    protected String[] buildRecord() {
        return compactFields();
    }

    /// A builder for [StringArrayHandler].
    public static final class StringArrayHandlerBuilder
        extends AbstractInternalCsvCallbackHandlerBuilder<StringArrayHandlerBuilder> {

        private StringArrayHandlerBuilder() {
        }

        @Override
        protected StringArrayHandlerBuilder self() {
            return this;
        }

        /// Builds the [StringArrayHandler] instance.
        ///
        /// @return the new instance
        /// @throws IllegalArgumentException if argument constraints are violated
        ///     (see [AbstractInternalCsvCallbackHandler])
        public StringArrayHandler build() {
            return new StringArrayHandler(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
        }

    }

}
