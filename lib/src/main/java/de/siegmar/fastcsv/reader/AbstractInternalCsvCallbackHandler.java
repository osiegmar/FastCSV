package de.siegmar.fastcsv.reader;

import java.util.Objects;

import de.siegmar.fastcsv.util.Preconditions;

/// Abstract base class for [CsvCallbackHandler] implementations.
///
/// @param <T> the type of the resulting records
public abstract sealed class AbstractInternalCsvCallbackHandler<T> extends CsvCallbackHandler<T>
    permits CsvRecordHandler, NamedCsvRecordHandler, StringArrayHandler {

    private static final int DEFAULT_INITIAL_FIELDS_SIZE = 32;

    /// The maximum number of fields a single record may have.
    protected final int maxFields;

    /// The maximum number of characters a single field may have.
    protected final int maxFieldSize;

    /// The maximum number of characters a single record may have.
    protected final int maxRecordSize;

    /// The field modifier.
    protected final FieldModifier fieldModifier;

    /// The starting line number of the current record.
    ///
    /// See [#beginRecord(long)].
    protected long startingLineNumber;

    /// The internal fields array.
    protected String[] fields;

    /// The total size (sum of all characters) of the current record.
    protected int recordSize;

    /// The current index in the internal fields array.
    protected int fieldIdx;

    /// Whether the current record is a comment.
    protected boolean comment;

    /// Whether the line is empty.
    protected boolean emptyLine;

    /// Constructs a new instance with the given configuration.
    ///
    /// @param maxFields     the maximum number of fields, must be greater than 0 and
    /// @param maxFieldSize  the maximum field size, must be greater than 0
    /// @param maxRecordSize the maximum record size, must be greater than 0
    /// @param fieldModifier the field modifier, must not be `null`
    /// @throws IllegalArgumentException if the arguments are invalid
    /// @throws NullPointerException     if `null` is passed
    protected AbstractInternalCsvCallbackHandler(final int maxFields,
                                                 final int maxFieldSize,
                                                 final int maxRecordSize,
                                                 final FieldModifier fieldModifier) {

        Preconditions.checkArgument(maxFields > 0, "maxFields must be > 0");
        Preconditions.checkArgument(maxFieldSize > 0, "maxFieldSize must be > 0");
        Preconditions.checkArgument(maxRecordSize > 0, "maxRecordSize must be > 0");
        Preconditions.checkArgument(maxRecordSize >= maxFieldSize, "maxRecordSize must be >= maxFieldSize");

        this.maxFields = maxFields;
        this.maxFieldSize = maxFieldSize;
        this.maxRecordSize = maxRecordSize;
        this.fieldModifier = Objects.requireNonNull(fieldModifier, "fieldModifier must not be null");
        fields = new String[Math.min(DEFAULT_INITIAL_FIELDS_SIZE, maxFields)];
    }

    @Override
    protected boolean isComment() {
        return comment;
    }

    @Override
    protected boolean isEmptyLine() {
        return emptyLine;
    }

    @Override
    protected int getFieldCount() {
        return fieldIdx;
    }

    /// {@inheritDoc}
    /// Resets the internal state of this handler.
    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    protected void beginRecord(final long startingLineNumber) {
        this.startingLineNumber = startingLineNumber;
        fieldIdx = 0;
        recordSize = 0;
        comment = false;
        emptyLine = true;
    }

    /// {@inheritDoc}
    /// Materializes the field value, apply field modifier, checks constraints and adds the field to the record.
    ///
    /// @throws CsvParseException if the addition exceeds the limit of record size or maximum fields count.
    @Override
    protected void addField(final char[] buf, final int offset, final int len, final boolean quoted) {
        final String modifiedField = modifyField(new String(buf, offset, len), quoted);
        final int modifiedFieldLength = modifiedField.length();

        if (maxFieldSize < modifiedFieldLength) {
            throw new CsvParseException(maxFieldSizeExceededMessage());
        }
        if (maxRecordSize < recordSize + modifiedFieldLength) {
            throw new CsvParseException(maxRecordSizeExceededMessage());
        }

        if (fieldIdx == fields.length) {
            extendCapacity();
        }

        emptyLine = emptyLine && fieldIdx == 0 && len == 0 && !quoted;
        fields[fieldIdx++] = modifiedField;
        recordSize += modifiedFieldLength;
    }

    /// Modifies field value.
    ///
    /// @param value  the field value
    /// @param quoted `true` if the field was quoted
    /// @return the modified field value
    protected String modifyField(final String value, final boolean quoted) {
        return fieldModifier.modify(startingLineNumber, fieldIdx, quoted, value);
    }

    private String maxFieldSizeExceededMessage() {
        return "Field at index %d in record starting at line %d exceeds the max field size of %d characters"
            .formatted(fieldIdx, startingLineNumber, maxFieldSize);
    }

    private String maxRecordSizeExceededMessage() {
        return "Field at index %d in record starting at line %d exceeds the max record size of %d characters"
            .formatted(fieldIdx, startingLineNumber, maxRecordSize);
    }

    /// {@inheritDoc}
    /// Materializes the comment value, apply field modifier, checks constraints and adds the field to the record.
    ///
    /// @throws CsvParseException if the addition exceeds the limit of record size.
    @Override
    protected void setComment(final char[] buf, final int offset, final int len) {
        if (fieldIdx != 0) {
            // CsvParser is aware that comments are one-field records, so this should never happen
            throw new IllegalStateException("Comment must be the first and only field in a record");
        }

        final String modifiedComment = modifyComment(new String(buf, offset, len));
        final int modifiedCommentLength = modifiedComment.length();
        if (maxFieldSize < modifiedCommentLength) {
            throw new CsvParseException(maxFieldSizeExceededMessage());
        }

        // No need to check maxRecordSize here since maxRecordSize >= maxFieldSize and
        // comments are one-field records

        recordSize += modifiedCommentLength;
        fields[fieldIdx++] = modifiedComment;
        comment = true;
        emptyLine = false;
    }

    /// Modifies comment value.
    ///
    /// @param field the comment value
    /// @return the modified comment value
    protected String modifyComment(final String field) {
        return fieldModifier.modifyComment(startingLineNumber, field);
    }

    private void extendCapacity() {
        if (fields.length == maxFields) {
            throw new CsvParseException("Record starting at line %d has surpassed the maximum limit of %d fields"
                .formatted(startingLineNumber, maxFields));
        }
        final String[] newFields = new String[Math.min(maxFields, fields.length * 2)];
        System.arraycopy(fields, 0, newFields, 0, fieldIdx);
        fields = newFields;
    }

    /// Builds a compact fields array (a copy of the internal fields array with the length of the current record).
    ///
    /// In contrast to the class property [#fields], the returned array does only contain the fields of the
    /// current record.
    ///
    /// @return the compact fields array
    protected String[] compactFields() {
        final String[] ret = new String[fieldIdx];
        System.arraycopy(fields, 0, ret, 0, fieldIdx);
        return ret;
    }

    /// Abstract builder for [AbstractInternalCsvCallbackHandler] subclasses.
    ///
    /// This class is for **internal use only** and should not be used directly. It will be sealed in a future release.
    ///
    /// @param <T> the type of the actual builder
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public abstract static class AbstractInternalCsvCallbackHandlerBuilder
        <T extends AbstractInternalCsvCallbackHandlerBuilder<?>> {

        private static final int DEFAULT_MAX_FIELDS = 16 * 1024;
        private static final int DEFAULT_MAX_FIELD_SIZE = 16 * 1024 * 1024;
        private static final int DEFAULT_MAX_RECORD_SIZE = 4 * DEFAULT_MAX_FIELD_SIZE;

        /// The maximum number of fields a single record may have.
        /// The default value is {@value %,2d #DEFAULT_MAX_FIELDS}.
        protected int maxFields = DEFAULT_MAX_FIELDS;

        /// The maximum number of characters a single field may have.
        /// The default value is {@value %,2d #DEFAULT_MAX_FIELD_SIZE}.
        protected int maxFieldSize = DEFAULT_MAX_FIELD_SIZE;

        /// The maximum number of characters a single record may have.
        /// The default value is {@value %,2d #DEFAULT_MAX_RECORD_SIZE}.
        protected int maxRecordSize = DEFAULT_MAX_RECORD_SIZE;

        /// The field modifier.
        /// The default value is [FieldModifiers#NOP].
        protected FieldModifier fieldModifier = FieldModifiers.NOP;

        /// Constructs a new default instance.
        protected AbstractInternalCsvCallbackHandlerBuilder() {
        }

        /// Method to be implemented by subclasses to return the correct type.
        ///
        /// @return This object of subclass type.
        protected abstract T self();

        /// Defines the maximum number of fields a single record may have.
        ///
        /// This constraint is enforced for all fields, including the header.
        ///
        /// @param maxFields the maximum fields a record may have, must be greater than 0
        ///                                                    (default: {@value %,2d #DEFAULT_MAX_FIELDS})
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if the argument is less than 1
        @SuppressWarnings("checkstyle:HiddenField")
        public T maxFields(final int maxFields) {
            Preconditions.checkArgument(maxFields > 0, "maxFields must be greater than 0");
            this.maxFields = maxFields;
            return self();
        }

        /// Defines the maximum number of characters a single field may have.
        ///
        /// This constraint is enforced for all fields, including the header and comments.
        /// The size of the field is determined **after** field modifiers are applied.
        ///
        /// In contrast to [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#maxBufferSize(int)] which enforces
        /// the maximum field size **before** the field modifier is applied, this constraint allows more precise control
        /// over the field size as field modifiers may have a significant impact on the field size.
        ///
        /// @param maxFieldSize the maximum field size, must be greater than 0
        ///                     (default: {@value %,2d #DEFAULT_MAX_FIELD_SIZE})
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if the argument is less than 1
        /// @see de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#maxBufferSize(int)
        @SuppressWarnings("checkstyle:HiddenField")
        public T maxFieldSize(final int maxFieldSize) {
            Preconditions.checkArgument(maxFieldSize > 0, "maxFieldSize must be greater than 0");
            this.maxFieldSize = maxFieldSize;
            return self();
        }

        /// Defines the maximum number of characters a single record may have.
        ///
        /// This constraint is enforced for all fields, including the header and comments.
        /// The size of the record is the sum of the sizes of all fields.
        /// The size of each field is determined **after** field modifiers are applied.
        ///
        /// Make sure that [#maxRecordSize] is greater than or equal to [#maxFieldSize].
        ///
        /// @param maxRecordSize the maximum record size, must be greater than 0
        ///                      (default: {@value %,2d #DEFAULT_MAX_RECORD_SIZE})
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws IllegalArgumentException if the argument is less than 1
        /// @see #maxFieldSize(int)
        @SuppressWarnings("checkstyle:HiddenField")
        public T maxRecordSize(final int maxRecordSize) {
            Preconditions.checkArgument(maxRecordSize > 0, "maxRecordSize must be greater than 0");
            this.maxRecordSize = maxRecordSize;
            return self();
        }

        /// Sets the field modifier.
        ///
        /// @param fieldModifier the field modifier, must not be `null` (default: [FieldModifiers#NOP])
        /// @return This updated object, allowing additional method calls to be chained together.
        /// @throws NullPointerException if `null` is passed
        @SuppressWarnings("checkstyle:HiddenField")
        public T fieldModifier(final FieldModifier fieldModifier) {
            Objects.requireNonNull(fieldModifier, "fieldModifier must not be null");
            this.fieldModifier = fieldModifier;
            return self();
        }

    }

}
