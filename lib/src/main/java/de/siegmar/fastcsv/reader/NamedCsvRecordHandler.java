package de.siegmar.fastcsv.reader;

import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final boolean allowDuplicateHeader;
    private String[] header;

    private NamedCsvRecordHandler(final int maxFields, final int maxFieldSize, final int maxRecordSize,
                                  final FieldModifier fieldModifier,
                                  final boolean allowDuplicateHeader, final List<String> header) {
        super(maxFields, maxFieldSize, maxRecordSize, fieldModifier);
        this.allowDuplicateHeader = allowDuplicateHeader;
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
    /// @throws NullPointerException     if `null` is passed
    /// @throws IllegalArgumentException if argument constraints are violated
    /// @see #builder()
    public static NamedCsvRecordHandler of(final Consumer<NamedCsvRecordHandlerBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer must not be null");
        final NamedCsvRecordHandlerBuilder builder = builder();
        configurer.accept(builder);
        return builder.build();
    }

    @SuppressWarnings("PMD.UseVarargs")
    private void setHeader(final String[] header) {
        Objects.requireNonNull(header, "header must not be null");
        for (final String h : header) {
            Objects.requireNonNull(h, "header element must not be null");
        }

        if (!allowDuplicateHeader) {
            checkForDuplicates(header);
        }

        this.header = header.clone();
    }

    @SuppressWarnings("PMD.UseVarargs")
    private static void checkForDuplicates(final String[] header) {
        final var duplicateHeaders = new LinkedHashSet<String>();
        final var seen = new HashSet<String>();
        for (final String h : header) {
            if (!seen.add(h)) {
                duplicateHeaders.add(h);
            }
        }

        if (!duplicateHeaders.isEmpty()) {
            throw new IllegalArgumentException("Header contains duplicate fields: "
                + duplicateHeaders);
        }
    }

    @Override
    protected NamedCsvRecord buildRecord() {
        if (comment) {
            return new NamedCsvRecord(startingLineNumber, compactFields(), true, EMPTY_HEADER);
        }

        if (header == null) {
            setHeader(compactFields());
            return null;
        }

        return new NamedCsvRecord(startingLineNumber, compactFields(), false, header);
    }

    /// A builder for [NamedCsvRecordHandler].
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class NamedCsvRecordHandlerBuilder
        extends AbstractInternalCsvCallbackHandlerBuilder<NamedCsvRecordHandlerBuilder> {

        private boolean allowDuplicateHeader;
        private List<String> header;

        private NamedCsvRecordHandlerBuilder() {
        }

        /// Sets whether duplicate header fields are allowed.
        ///
        /// When set to `false`, an [IllegalArgumentException] is thrown if the header contains duplicate fields.
        /// When set to `true`, duplicate fields are allowed. See [NamedCsvRecord] for details on how duplicate
        /// headers are handled.
        ///
        /// @param allowDuplicateHeader whether duplicate header fields are allowed (default: `false`)
        /// @return This updated object, allowing additional method calls to be chained together.
        public NamedCsvRecordHandlerBuilder allowDuplicateHeader(final boolean allowDuplicateHeader) {
            this.allowDuplicateHeader = allowDuplicateHeader;
            return this;
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
            return new NamedCsvRecordHandler(maxFields, maxFieldSize, maxRecordSize, fieldModifier,
                allowDuplicateHeader, header);
        }

    }

}
