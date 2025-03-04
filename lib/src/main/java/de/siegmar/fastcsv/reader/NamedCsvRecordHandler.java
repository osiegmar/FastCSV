package de.siegmar.fastcsv.reader;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/// A callback handler that returns a [NamedCsvRecord] for each record.
///
/// Example:
/// ```
/// NamedCsvRecordHandler handler = NamedCsvRecordHandler.builder()
///     .fieldModifier(FieldModifiers.TRIM)
///     .header("foo", "bar")
///     .build();
/// ```
///
/// This implementation is stateful and must not be reused.
public final class NamedCsvRecordHandler extends AbstractInternalCsvCallbackHandler<NamedCsvRecord> {

    private static final String[] EMPTY_HEADER = new String[0];
    private String[] header;

    /// Constructs a new [NamedCsvRecordHandler] with an empty header.
    ///
    /// @deprecated Use [#of()] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler() {
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header.
    ///
    /// @param header the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler(final List<String> header) {
        setHeader(header.toArray(new String[0]));
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header.
    ///
    /// @param header the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler(final String... header) {
        setHeader(header);
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler(final FieldModifier fieldModifier) {
        super(fieldModifier);
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header and field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @param header        the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler(final FieldModifier fieldModifier, final List<String> header) {
        super(fieldModifier);
        setHeader(header.toArray(new String[0]));
    }

    /// Constructs a new [NamedCsvRecordHandler] with the given header and field modifier.
    ///
    /// @param fieldModifier the field modifier, must not be `null`
    /// @param header        the header, must not be `null` or contain `null` elements
    /// @throws NullPointerException if `null` is passed
    /// @deprecated Use [#builder()] or [#of(Consumer)] instead.
    @SuppressWarnings("removal")
    @Deprecated(since = "3.6.0", forRemoval = true)
    public NamedCsvRecordHandler(final FieldModifier fieldModifier, final String... header) {
        super(fieldModifier);
        setHeader(header);
    }

    private NamedCsvRecordHandler(final int maxFields, final int maxFieldSize, final int maxRecordSize,
                                  final FieldModifier fieldModifier, final List<String> header) {
        super(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
        if (header != null) {
            setHeader(header.toArray(new String[0]));
        }
    }

    /// Constructs a new builder instance for this class.
    ///
    /// @return the builder
    /// @see #of(Consumer)
    public static NamedCsvRecordHandlerBuilder builder() {
        return new NamedCsvRecordHandlerBuilder();
    }

    /// Constructs a new instance of this class with default settings.
    ///
    /// @return the new instance
    /// @see NamedCsvRecordHandlerBuilder#build()
    public static NamedCsvRecordHandler of() {
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
    public static NamedCsvRecordHandler of(final Consumer<NamedCsvRecordHandlerBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        final NamedCsvRecordHandlerBuilder builder = builder();
        configurer.accept(builder);
        return builder.build();
    }

    private void setHeader(final String... header) {
        Objects.requireNonNull(header, "header must not be null");
        for (final String h : header) {
            Objects.requireNonNull(h, "header element must not be null");
        }
        this.header = header.clone();
    }

    @Override
    protected RecordWrapper<NamedCsvRecord> buildRecord() {
        if (comment) {
            return buildWrapper(new NamedCsvRecord(startingLineNumber, compactFields(), true, EMPTY_HEADER));
        }

        if (header == null) {
            setHeader(compactFields());
            return null;
        }

        return buildWrapper(new NamedCsvRecord(startingLineNumber, compactFields(), false, header));
    }

    /// A builder for [NamedCsvRecordHandler].
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static final class NamedCsvRecordHandlerBuilder
        extends AbstractInternalCsvCallbackHandlerBuilder<NamedCsvRecordHandlerBuilder> {

        private List<String> header;

        private NamedCsvRecordHandlerBuilder() {
        }

        /// Sets a predefined header.
        ///
        /// When not set, the header is taken from the first record (that is not a comment).
        ///
        /// @param header the header, must not be `null`
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws NullPointerException if `null` is passed
        /// @see #header(List)
        @SuppressWarnings("checkstyle:HiddenField")
        public NamedCsvRecordHandlerBuilder header(final String... header) {
            Objects.requireNonNull(header, "header must not be null");
            this.header = List.of(header);
            return this;
        }

        /// Sets the header.
        ///
        /// When not set, the header is taken from the first record (that is not a comment).
        ///
        /// @param header the header, must not be `null`
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws NullPointerException if `null` is passed
        /// @see #header(String...)
        @SuppressWarnings("checkstyle:HiddenField")
        public NamedCsvRecordHandlerBuilder header(final List<String> header) {
            Objects.requireNonNull(header, "header must not be null");
            this.header = List.copyOf(header);
            return this;
        }

        @Override
        protected NamedCsvRecordHandlerBuilder self() {
            return this;
        }

        /// Builds the [NamedCsvRecordHandler] instance.
        ///
        /// @return the new instance
        /// @throws IllegalArgumentException if argument constraints are violated
        ///     (see [AbstractInternalCsvCallbackHandler])
        public NamedCsvRecordHandler build() {
            return new NamedCsvRecordHandler(maxFields, maxFieldSize, maxRecordSize, fieldModifier, header);
        }

    }

}
