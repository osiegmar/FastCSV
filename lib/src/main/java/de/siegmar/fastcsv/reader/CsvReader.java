package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.util.Util.containsDupe;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/**
 * This is the main class for reading CSV data.
 * <p>
 * Example use:
 * {@snippet :
 * try (CsvReader<CsvRecord> csv = CsvReader.builder().build(file)) {
 *     for (CsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 * <p>
 * Example for named records:
 * {@snippet :
 * try (CsvReader<NamedCsvRecord> csv = CsvReader.builder().build(file, CsvCallbackHandlers.ofNamedCsvRecord())) {
 *     for (NamedCsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 *
 * @param <T> the type of the CSV record.
 */
public final class CsvReader<T> implements Iterable<T>, Closeable {

    private final CsvCallbackHandler<T> recordHandler;
    private final RecordReader recordReader;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyLines;
    private final boolean ignoreDifferentFieldCount;
    private final CloseableIterator<T> csvRecordIterator = new CsvRecordIterator();

    private int firstRecordFieldCount = -1;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvReader(final Reader reader, final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean skipEmptyLines, final boolean ignoreDifferentFieldCount,
              final CsvCallbackHandler<T> recordHandler, final FieldModifier fieldModifier) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.recordHandler = recordHandler;
        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;

        recordReader = new RecordReader(recordHandler, fieldModifier, reader, fieldSeparator, quoteCharacter,
            commentStrategy, commentCharacter);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvReader(final String data, final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean skipEmptyLines, final boolean ignoreDifferentFieldCount,
              final CsvCallbackHandler<T> recordHandler, final FieldModifier fieldModifier) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.recordHandler = recordHandler;
        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;

        recordReader = new RecordReader(recordHandler, fieldModifier, data, fieldSeparator, quoteCharacter,
            commentStrategy, commentCharacter);
    }

    private void assertFields(final char fieldSeparator, final char quoteCharacter, final char commentCharacter) {
        Preconditions.checkArgument(!Util.isNewline(fieldSeparator), "fieldSeparator must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(quoteCharacter), "quoteCharacter must not be a newline char");
        Preconditions.checkArgument(!Util.isNewline(commentCharacter), "commentCharacter must not be a newline char");
        Preconditions.checkArgument(!containsDupe(fieldSeparator, quoteCharacter, commentCharacter),
            "Control characters must differ"
                + " (fieldSeparator=%s, quoteCharacter=%s, commentCharacter=%s)",
            fieldSeparator, quoteCharacter, commentCharacter);
    }

    /**
     * Constructs a {@link CsvReaderBuilder} to configure and build instances of this class.
     *
     * @return a new {@link CsvReaderBuilder} instance.
     */
    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    /**
     * Returns an iterator over elements of type {@link CsvRecord}.
     * <p>
     * The returned iterator is not thread-safe.
     * Don't forget to close the returned iterator when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent.
     *
     * @return an iterator over the CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException    if any other problem occurs when parsing the CSV data.
     * @see #stream()
     */
    @Override
    public CloseableIterator<T> iterator() {
        return csvRecordIterator;
    }

    /**
     * Returns a {@link Spliterator} over elements of type {@link CsvRecord}.
     * <p>
     * The returned spliterator is not thread-safe.
     * Don't forget to invoke {@link #close()} when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent.
     *
     * @return a spliterator over the CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException    if any other problem occurs when parsing the CSV data.
     * @see #stream()
     */
    @Override
    public Spliterator<T> spliterator() {
        return new CsvSpliterator();
    }

    /**
     * Returns a sequential {@code Stream} with this reader as its source.
     * <p>
     * The returned stream is not thread-safe.
     * Don't forget to close the returned stream when you're done.
     * <br>
     * This method is idempotent.
     *
     * @return a sequential {@code Stream} over the CSV records.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException    if any other problem occurs when parsing the CSV data.
     * @see #iterator()
     */
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false)
            .onClose(() -> {
                try {
                    close();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    @SuppressWarnings({
        "PMD.AvoidBranchingStatementAsLastInLoop",
        "PMD.AssignmentInOperand"
    })
    private T fetchRecord() throws IOException {
        while (recordReader.read()) {
            final T csvRecord = recordHandler.buildRecord();

            if (csvRecord == null) {
                // data was consumed (e.g. header for named records)
                continue;
            }

            if (recordHandler.isComment()) {
                if (commentStrategy == CommentStrategy.SKIP) {
                    // skip commented records
                    continue;
                }

                return csvRecord;
            }

            // skip empty lines
            if (recordHandler.isEmptyLine()) {
                if (skipEmptyLines) {
                    continue;
                }
            } else if (!ignoreDifferentFieldCount) {
                final int fieldCount = recordHandler.getFieldCount();

                // check the field count consistency on every record
                if (firstRecordFieldCount == -1) {
                    firstRecordFieldCount = fieldCount;
                } else if (fieldCount != firstRecordFieldCount) {
                    throw new CsvParseException(
                        String.format("Record %d has %d fields, but first record had %d fields",
                            recordReader.getStartingLineNumber(), fieldCount, firstRecordFieldCount));
                }
            }

            return csvRecord;
        }

        recordHandler.terminate();

        return null;
    }

    @Override
    public void close() throws IOException {
        recordReader.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvReader.class.getSimpleName() + "[", "]")
            .add("commentStrategy=" + commentStrategy)
            .add("skipEmptyLines=" + skipEmptyLines)
            .add("ignoreDifferentFieldCount=" + ignoreDifferentFieldCount)
            .toString();
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "PMD.AvoidCatchingThrowable"})
    private T fetch() {
        try {
            return fetchRecord();
        } catch (final IOException e) {
            throw new UncheckedIOException(buildExceptionMessage(), e);
        } catch (final Throwable t) {
            throw new CsvParseException(buildExceptionMessage(), t);
        }
    }

    private String buildExceptionMessage() {
        return (recordReader.getStartingLineNumber() == 1)
            ? "Exception when reading first record"
            : String.format("Exception when reading record that started in line %d",
            recordReader.getStartingLineNumber());
    }

    private class CsvSpliterator implements Spliterator<T> {

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            final T t = fetch();
            if (t != null) {
                action.accept(t);
                return true;
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
        }

    }

    private class CsvRecordIterator implements CloseableIterator<T> {

        private T fetchedRecord;
        private boolean fetched;

        @Override
        public boolean hasNext() {
            if (!fetched) {
                fetchedRecord = fetch();
                fetched = true;
            }
            return fetchedRecord != null;
        }

        @Override
        public T next() {
            if (!fetched) {
                fetchedRecord = fetch();
            }
            if (fetchedRecord == null) {
                throw new NoSuchElementException();
            }

            fetched = false;
            return fetchedRecord;
        }

        @Override
        public void close() throws IOException {
            CsvReader.this.close();
        }

    }

    /**
     * This builder is used to create configured instances of {@link CsvReader}. The default
     * configuration of this class complies with RFC 4180:
     * <ul>
     *     <li>Field separator: {@code ,} (comma)</li>
     *     <li>Quote character: {@code "} (double quotes)</li>
     *     <li>Comment strategy: {@link CommentStrategy#NONE} (as RFC doesn't handle comments)</li>
     *     <li>Comment character: {@code #} (hash) (in case comment strategy is enabled)</li>
     *     <li>Skip empty lines: {@code true}</li>
     *     <li>Ignore different field count: {@code true}</li>
     *     <li>Detect BOM header: {@code false}</li>
     * </ul>
     * <p>
     * The line delimiter (line-feed, carriage-return or the combination of both) is detected
     * automatically and thus not configurable.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    public static final class CsvReaderBuilder {

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean skipEmptyLines = true;
        private boolean ignoreDifferentFieldCount = true;
        private boolean detectBomHeader;
        private FieldModifier fieldModifier;

        private CsvReaderBuilder() {
        }

        /**
         * Sets the {@code fieldSeparator} used when reading CSV data.
         *
         * @param fieldSeparator the field separator character (default: {@code ,} - comma).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /**
         * Sets the {@code quoteCharacter} used when reading CSV data.
         *
         * @param quoteCharacter the character used to enclose fields
         *                       (default: {@code "} - double quotes).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines how (and if) commented lines should be handled
         * (default: {@link CommentStrategy#NONE} as comments are not defined in RFC 4180).
         * <p>
         * If a comment strategy other than {@link CommentStrategy#NONE} is used, special parsing rules are
         * applied for commented lines. FastCSV defines a comment as a line that starts with a comment character.
         * No (whitespace) character is allowed before the comment character. Everything after the comment character
         * until the end of the line is considered the comment value.
         *
         * @param commentStrategy the strategy for handling comments.
         * @return This updated object, allowing additional method calls to be chained together.
         * @see #commentCharacter(char)
         */
        public CsvReaderBuilder commentStrategy(final CommentStrategy commentStrategy) {
            this.commentStrategy = commentStrategy;
            return this;
        }

        /**
         * Sets the {@code commentCharacter} used to comment lines.
         *
         * @param commentCharacter the character used to comment lines (default: {@code #} - hash)
         * @return This updated object, allowing additional method calls to be chained together.
         * @see #commentStrategy(CommentStrategy)
         */
        public CsvReaderBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Defines whether empty lines should be skipped when reading data.
         * <p>
         * The default implementation interprets empty lines as lines that do not contain any data.
         * This includes lines that consist only of opening and closing quote characters.
         * <p>
         * A line that only contains whitespace characters is not considered empty.
         * However, the determination of empty lines is done after field modifiers have been applied.
         * If you use a field trimming modifier (like {@link FieldModifiers#TRIM}), lines that only contain whitespaces
         * are considered empty.
         * <p>
         * Commented lines are not considered empty lines. Use {@link #commentStrategy(CommentStrategy)} for handling
         * commented lines.
         *
         * @param skipEmptyLines Whether empty lines should be skipped (default: {@code true}).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder skipEmptyLines(final boolean skipEmptyLines) {
            this.skipEmptyLines = skipEmptyLines;
            return this;
        }

        /**
         * Defines if an {@link CsvParseException} should be thrown if records do contain a
         * different number of fields.
         *
         * @param ignoreDifferentFieldCount if exception should be suppressed, when CSV data contains
         *                                  different field count (default: {@code true}).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder ignoreDifferentFieldCount(final boolean ignoreDifferentFieldCount) {
            this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;
            return this;
        }

        /**
         * Defines if an optional BOM (Byte order mark) header should be detected.
         * BOM detection only applies for direct file access.
         * <p>
         * Supported BOMs are: UTF-8, UTF-16LE, UTF-16BE, UTF-32LE, UTF-32BE.
         *
         * @param detectBomHeader if detection should be enabled (default: {@code false})
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder detectBomHeader(final boolean detectBomHeader) {
            this.detectBomHeader = detectBomHeader;
            return this;
        }

        /**
         * Registers an optional field modifier. Used to modify the field values.
         * By default, no field modifier is used.
         * <p>
         * Example:
         * {@snippet :
         * var fields = CsvReader.builder()
         *     .fieldModifier(FieldModifiers.TRIM.andThen(FieldModifiers.upper(Locale.ENGLISH)))
         *     .build("  foo   ,   bar")
         *     .stream()
         *     .collect(Collectors.toList());
         *
         * // fields would be: "FOO" and "BAR"
         *}
         * <p>
         * Applying field modifiers might affect the behavior of skipping empty lines – see
         * {@link #skipEmptyLines(boolean)}.
         *
         * @param fieldModifier the modifier to use.
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder fieldModifier(final FieldModifier fieldModifier) {
            this.fieldModifier = fieldModifier;
            return this;
        }

        /**
         * Constructs a new {@link CsvReader} that uses {@link CsvRecord} as record type.
         * <p>
         * This is a convenience method for calling {@code build(reader, CsvCallbackHandlers.ofCsvRecord())}.
         *
         * @param reader the data source to read from.
         * @return a new CsvReader of CsvRecord - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
         */
        public CsvReader<CsvRecord> build(final Reader reader) {
            return build(reader, CsvCallbackHandlers.ofCsvRecord());
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered Reader
         * implementation such as {@link java.io.BufferedReader}. Performance may be even likely
         * better if you do not.
         * <p>
         * Use {@link #build(Path, Charset)} for optimal performance when
         * reading files and {@link #build(String)} when reading Strings.
         *
         * @param <T>           the type of the CSV record.
         * @param reader        the data source to read from.
         * @param recordHandler the record handler to use. Do not reuse a handler after it has been used!
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
         */
        public <T> CsvReader<T> build(final Reader reader, final CsvCallbackHandler<T> recordHandler) {
            return newReader(Objects.requireNonNull(reader, "reader must not be null"), recordHandler);
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         * <p>
         * This is a convenience method for calling {@code build(data, CsvCallbackHandlers.ofCsvRecord())}.
         *
         * @param data the data to read.
         * @return a new CsvReader of CsvRecord - never {@code null}.
         */
        public CsvReader<CsvRecord> build(final String data) {
            return build(data, CsvCallbackHandlers.ofCsvRecord());
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param <T>           the type of the CSV record.
         * @param data          the data to read.
         * @param recordHandler the record handler to use. Do not reuse a handler after it has been used!
         * @return a new CsvReader - never {@code null}.
         */
        public <T> CsvReader<T> build(final String data, final CsvCallbackHandler<T> recordHandler) {
            return newReader(Objects.requireNonNull(data, "data must not be null"), recordHandler);
        }

        /**
         * Constructs a new {@link CsvReader} for the specified file.
         * <p>
         * This is a convenience method for calling {@code build(file, CsvCallbackHandlers.ofCsvRecord())}.
         *
         * @param file the file to read data from.
         * @return a new CsvReader of CsvRecord - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file is {@code null}
         */
        public CsvReader<CsvRecord> build(final Path file) throws IOException {
            return build(file, CsvCallbackHandlers.ofCsvRecord());
        }

        /**
         * Constructs a new {@link CsvReader} for the specified file.
         * <p>
         * This is a convenience method for calling {@code build(file, StandardCharsets.UTF_8, recordHandler)}.
         *
         * @param <T>           the type of the CSV record.
         * @param file          the file to read data from.
         * @param recordHandler the record handler to use. Do not reuse a handler after it has been used!
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public <T> CsvReader<T> build(final Path file, final CsvCallbackHandler<T> recordHandler) throws IOException {
            return build(file, StandardCharsets.UTF_8, recordHandler);
        }

        /**
         * Constructs a new {@link CsvReader} for the specified file.
         * <p>
         * This is a convenience method for calling {@code build(file, charset, CsvCallbackHandlers.ofCsvRecord())}.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use. If BOM header detection is enabled
         *                (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                when no BOM header was found.
         * @return a new CsvReader of CsvRecord - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvReader<CsvRecord> build(final Path file, final Charset charset) throws IOException {
            return build(file, charset, CsvCallbackHandlers.ofCsvRecord());
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param <T>           the type of the CSV record.
         * @param file          the file to read data from.
         * @param charset       the character set to use. If BOM header detection is enabled
         *                      (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                      when no BOM header was found.
         * @param recordHandler the record handler to use. Do not reuse a handler after it has been used!
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public <T> CsvReader<T> build(final Path file, final Charset charset, final CsvCallbackHandler<T> recordHandler)
            throws IOException {

            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");
            Objects.requireNonNull(recordHandler, "recordHandler must not be null");

            final Reader reader = detectBomHeader
                ? BomUtil.openReader(file, charset)
                : new InputStreamReader(Files.newInputStream(file), charset);

            return newReader(reader, recordHandler);
        }

        private <T> CsvReader<T> newReader(final Reader reader, final CsvCallbackHandler<T> recordHandler) {
            return new CsvReader<>(reader, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, skipEmptyLines, ignoreDifferentFieldCount, recordHandler, fieldModifier);
        }

        private <T> CsvReader<T> newReader(final String data, final CsvCallbackHandler<T> recordHandler) {
            return new CsvReader<>(data, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, skipEmptyLines, ignoreDifferentFieldCount, recordHandler, fieldModifier);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CsvReaderBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentStrategy=" + commentStrategy)
                .add("commentCharacter=" + commentCharacter)
                .add("skipEmptyLines=" + skipEmptyLines)
                .add("ignoreDifferentFieldCount=" + ignoreDifferentFieldCount)
                .add("fieldModifier=" + fieldModifier)
                .toString();
        }

    }

}
