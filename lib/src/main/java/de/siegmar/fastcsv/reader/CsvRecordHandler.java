package de.siegmar.fastcsv.reader;

import java.util.Objects;
import java.util.function.Consumer;

/// A [CsvCallbackHandler] implementation that returns a [CsvRecord] for each record.
///
/// Example:
/// ```
/// CsvRecordHandler handler = CsvRecordHandler.builder()
///     .fieldModifier(FieldModifiers.TRIM)
///     .build();
/// ```
///
/// This implementation is stateful and must not be reused.
public final class CsvRecordHandler extends AbstractInternalCsvCallbackHandler<CsvRecord> {

    /// Constructs a new [CsvRecordHandler].
    ///
    /// @deprecated Use [#of()] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public CsvRecordHandler() {
        super();
    }

    /// Constructs a new [CsvRecordHandler] with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public CsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    private CsvRecordHandler(final int maxFields, final int maxFieldSize, final int maxRecordSize,
                             final FieldModifier fieldModifier) {
        super(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
    }

    /// Constructs a new builder instance for this class.
    ///
    /// @return the builder
    /// @see #of(Consumer)
    public static CsvRecordHandlerBuilder builder() {
        return new CsvRecordHandlerBuilder();
    }

    /// Constructs a new instance of this class with default settings.
    ///
    /// @return the new instance
    /// @see CsvRecordHandlerBuilder#build()
    public static CsvRecordHandler of() {
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
    public static CsvRecordHandler of(final Consumer<CsvRecordHandlerBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        final CsvRecordHandlerBuilder builder = builder();
        configurer.accept(builder);
        return builder.build();
    }

    @Override
    protected RecordWrapper<CsvRecord> buildRecord() {
        return buildWrapper(new CsvRecord(startingLineNumber, compactFields(), comment));
    }

    /// A builder for [CsvRecordHandler].
    public static final class CsvRecordHandlerBuilder
        extends AbstractInternalCsvCallbackHandlerBuilder<CsvRecordHandlerBuilder> {

        private CsvRecordHandlerBuilder() {
        }

        @Override
        protected CsvRecordHandlerBuilder self() {
            return this;
        }

        /// Builds the [CsvRecordHandler] instance.
        ///
        /// @return the new instance
        /// @throws IllegalArgumentException if argument constraints are violated
        ///     (see [AbstractInternalCsvCallbackHandler])
        public CsvRecordHandler build() {
            return new CsvRecordHandler(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
        }

    }

}
