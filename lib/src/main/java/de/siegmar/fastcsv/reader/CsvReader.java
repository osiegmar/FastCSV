package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is the main class for reading CSV data.
 * <p>
 * The CSV records are read iteratively, regardless of whether the Iterable, the Iterator, or the Stream is used.
 * Once all records are read, the data is consumed. If you need to repeatedly read records, you should collect the
 * records in a List or another collection.
 * <p>
 * Example use:
 * {@snippet :
 * try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(file)) {
 *     for (CsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 * <p>
 * Example for named records:
 * {@snippet :
 * try (CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(file)) {
 *     for (NamedCsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 *
 * @param <T> the type of the CSV record.
 */
public final class CsvReader<T> implements Iterable<T>, Closeable {

    private final CsvParser csvParser;
    private final CsvCallbackHandler<T> callbackHandler;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyLines;
    private final boolean ignoreDifferentFieldCount;
    private final CloseableIterator<T> csvRecordIterator = new CsvRecordIterator();

    private int firstRecordFieldCount = -1;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvReader(final CsvParser csvParser, final CsvCallbackHandler<T> callbackHandler,
              final CommentStrategy commentStrategy, final boolean skipEmptyLines,
              final boolean ignoreDifferentFieldCount) {

        this.csvParser = csvParser;
        this.callbackHandler = callbackHandler;
        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;
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
     * Skips the specified number of lines.
     * <p>
     * The setting {@link CsvReaderBuilder#skipEmptyLines(boolean)} has no effect on this method.
     *
     * @param lineCount the number of lines to skip.
     * @throws IllegalArgumentException if lineCount is negative.
     * @throws UncheckedIOException     if an I/O error occurs.
     * @throws CsvParseException        if not enough lines are available to skip.
     */
    public void skipLines(final int lineCount) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount must be non-negative");
        }

        try {
            for (int i = 0; i < lineCount; i++) {
                if (!csvParser.skipLine(0)) {
                    throw new CsvParseException("Not enough lines to skip. Skipped only " + i + " line(s).");
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Skip lines until the specified predicate matches.
     * The line that matches the predicate is not skipped.
     * <p>
     * The method returns the number of lines actually skipped.
     * <p>
     * The setting {@link CsvReaderBuilder#skipEmptyLines(boolean)} has no effect on this method.
     *
     * @param predicate the predicate to match the lines.
     * @param maxLines  the maximum number of lines to skip.
     * @return the number of lines actually skipped.
     * @throws NullPointerException if predicate is {@code null}.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws CsvParseException if no matching line is found within the maximum limit of maxLines.
     */
    public int skipLines(final Predicate<String> predicate, final int maxLines) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        if (maxLines < 0) {
            throw new IllegalArgumentException("maxLines must be non-negative");
        }

        if (maxLines == 0) {
            return 0;
        }

        try {
            for (int i = 0; i < maxLines; i++) {
                final String line = csvParser.peekLine();
                if (predicate.test(line)) {
                    return i;
                }

                if (!csvParser.skipLine(line.length())) {
                    throw new CsvParseException(String.format(
                        "No matching line found. Skipped %d line(s) before reaching end of data.", i));
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        throw new CsvParseException(String.format(
            "No matching line found within the maximum limit of %d lines.", maxLines));
    }

    /**
     * Returns an iterator over elements of type {@link CsvRecord}.
     * <p>
     * The returned iterator is not thread-safe.
     * Remember to close the returned iterator when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent and can be called multiple times.
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
     * Remember to invoke {@link #close()} when you're done.
     * Alternatively, use {@link #stream()}.
     * <br>
     * This method is idempotent and can be called multiple times.
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
     * Returns a new sequential {@code Stream} with this reader as its source.
     * <p>
     * The returned stream is not thread-safe.
     * Remember to close the returned stream when you're done.
     * Closing the stream will also close this reader.
     * <br>
     * This method can be called multiple times, although it creates a new stream each time.
     * <br>
     * The stream is not reusable after it has been closed.
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
        while (csvParser.parse()) {
            final T csvRecord = processRecord();

            if (csvRecord != null) {
                return csvRecord;
            }
        }

        callbackHandler.terminate();

        return null;
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    private T processRecord() {
        final RecordWrapper<T> recordWrapper = callbackHandler.buildRecord();

        // handle consumed records (e.g., header for named records)
        if (recordWrapper == null) {
            return null;
        }

        // handle comment lines
        if (recordWrapper.isComment()) {
            return commentStrategy == CommentStrategy.SKIP ? null : recordWrapper.getWrappedRecord();
        }

        // handle empty lines
        if (recordWrapper.isEmptyLine()) {
            return skipEmptyLines ? null : recordWrapper.getWrappedRecord();
        }

        // check field count consistency
        if (!ignoreDifferentFieldCount) {
            checkFieldCountConsistency(recordWrapper.getFieldCount());
        }

        return recordWrapper.getWrappedRecord();
    }

    private void checkFieldCountConsistency(final int fieldCount) {
        // check the field count consistency on every record
        if (firstRecordFieldCount == -1) {
            firstRecordFieldCount = fieldCount;
        } else if (fieldCount != firstRecordFieldCount) {
            throw new CsvParseException(
                String.format("Record %d has %d fields, but first record had %d fields",
                    csvParser.getStartingLineNumber(), fieldCount, firstRecordFieldCount));
        }
    }

    @Override
    public void close() throws IOException {
        csvParser.close();
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
        return (csvParser.getStartingLineNumber() == 1)
            ? "Exception when reading first record"
            : String.format("Exception when reading record that started in line %d",
            csvParser.getStartingLineNumber());
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
            return ORDERED | NONNULL;
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
     * configuration of this class adheres with RFC 4180:
     * <ul>
     *     <li>Field separator: {@code ,} (comma)</li>
     *     <li>Quote character: {@code "} (double quotes)</li>
     *     <li>Comment strategy: {@link CommentStrategy#NONE} (as RFC doesn't handle comments)</li>
     *     <li>Comment character: {@code #} (hash) (in case comment strategy is enabled)</li>
     *     <li>Skip empty lines: {@code true}</li>
     *     <li>Ignore different field count: {@code true}</li>
     *     <li>Accept characters after quotes: {@code true}</li>
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
        private boolean acceptCharsAfterQuotes = true;
        private boolean detectBomHeader;

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
         * The default implementation interprets empty lines as lines that do not contain any data
         * (no whitespace, no quotes, nothing).
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
         * Specifies whether the presence of characters between a closing quote and a field separator or
         * the end of a line should be treated as an error or not.
         * <p>
         * Example:
         * <pre>
         * {@code "a"b,"c"}
         * </pre>
         * <p>
         * If this is set to {@code true}, the value {@code ab} will be returned for the first field.
         * <p>
         * If this is set to {@code false}, a {@link CsvParseException} will be thrown.
         *
         * @param acceptCharsAfterQuotes allow characters after quotes (default: {@code true}).
         * @return This updated object, allowing additional method calls to be chained together.
         */
        public CsvReaderBuilder acceptCharsAfterQuotes(final boolean acceptCharsAfterQuotes) {
            this.acceptCharsAfterQuotes = acceptCharsAfterQuotes;
            return this;
        }

        /**
         * Defines if an optional BOM (Byte order mark) header should be detected.
         * <p>
         * <strong>BOM detection only applies for {@link InputStream} and
         * {@link Path} based data sources.
         * It does not apply for {@link Reader} or {@link String} based data sources
         * as they are already decoded.</strong>
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
         * Constructs a new index-based {@link CsvReader} for the specified input stream.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, InputStream)} with
         * {@link CsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         *
         * @param inputStream the input stream to read data from.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if inputStream is {@code null}
         * @see #ofCsvRecord(InputStream, Charset)
         */
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream) throws IOException {
            return build(new CsvRecordHandler(), inputStream);
        }

        /**
         * Constructs a new index-based {@link CsvReader} for the specified input stream and character set.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, InputStream, Charset)} with
         * {@link CsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         *
         * @param inputStream the input stream to read data from.
         * @param charset     the character set to use. If BOM header detection is enabled
         *                    (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                    when no BOM header was found.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if inputStream or charset is {@code null}
         * @see #ofCsvRecord(InputStream)
         */
        public CsvReader<CsvRecord> ofCsvRecord(final InputStream inputStream, final Charset charset)
            throws IOException {
            return build(new CsvRecordHandler(), inputStream, charset);
        }

        /**
         * Constructs a new index-based {@link CsvReader} for the specified reader.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Reader)} with
         * {@link CsvRecordHandler} as callback handler.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param reader the data source to read from.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
         */
        public CsvReader<CsvRecord> ofCsvRecord(final Reader reader) {
            return build(new CsvRecordHandler(), reader);
        }

        /**
         * Constructs a new index-based {@link CsvReader} for the specified String.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, String)} with
         * {@link CsvRecordHandler} as callback handler.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param data the data to read.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if data is {@code null}
         */
        public CsvReader<CsvRecord> ofCsvRecord(final String data) {
            return build(new CsvRecordHandler(), data);
        }

        /**
         * Constructs a new index-based {@link CsvReader} for the specified file.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Path)} with
         * {@link CsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, the character set is determined by the BOM header.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         *
         * @param file the file to read data from.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file is {@code null}
         * @see #ofCsvRecord(Path, Charset)
         */
        public CsvReader<CsvRecord> ofCsvRecord(final Path file) throws IOException {
            return build(new CsvRecordHandler(), file);
        }

        /**
         * Constructs a new index-based {@link CsvReader} for the specified file and character set.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Path, Charset)} with
         * {@link CsvRecordHandler} as callback handler.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use. If BOM header detection is enabled
         *                (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                when no BOM header was found.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         * @see #ofCsvRecord(Path)
         */
        public CsvReader<CsvRecord> ofCsvRecord(final Path file, final Charset charset) throws IOException {
            return build(new CsvRecordHandler(), file, charset);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified input stream.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, InputStream)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         *
         * @param inputStream the input stream to read data from.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if reader is {@code null}
         * @see #ofNamedCsvRecord(InputStream, Charset)
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream) throws IOException {
            return build(new NamedCsvRecordHandler(), inputStream);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified input stream and character set.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, InputStream, Charset)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         *
         * @param inputStream the input stream to read data from.
         * @param charset     the character set to use. If BOM header detection is enabled
         *                    (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                    when no BOM header was found.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         * @see #ofNamedCsvRecord(InputStream)
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final InputStream inputStream, final Charset charset)
            throws IOException {
            return build(new NamedCsvRecordHandler(), inputStream, charset);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified reader.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Reader)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param reader the data source to read from.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Reader reader) {
            return build(new NamedCsvRecordHandler(), reader);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified String.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, String)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param data the data to read.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if data is {@code null}
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final String data) {
            return build(new NamedCsvRecordHandler(), data);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified file.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Path)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, the character set is determined by the BOM header.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         *
         * @param file the file to read data from.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file is {@code null}
         * @see #ofNamedCsvRecord(Path, Charset)
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Path file) throws IOException {
            return build(new NamedCsvRecordHandler(), file);
        }

        /**
         * Constructs a new name-based {@link CsvReader} for the specified file and character set.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, Path, Charset)} with
         * {@link NamedCsvRecordHandler} as callback handler.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use. If BOM header detection is enabled
         *                (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                when no BOM header was found.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         * @see #ofNamedCsvRecord(Path)
         */
        public CsvReader<NamedCsvRecord> ofNamedCsvRecord(final Path file, final Charset charset)
            throws IOException {
            return build(new NamedCsvRecordHandler(), file, charset);
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified input stream.
         * <p>
         * This is a convenience method for calling {@link #build(CsvCallbackHandler, InputStream, Charset)}.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered InputStream
         * implementation such as {@link java.io.BufferedInputStream}. Performance may be even likely
         * better if you do not.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         * <p>
         * Use {@link #build(CsvCallbackHandler, Path)} for optimal performance when reading files.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param inputStream     the input stream to read data from.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if callbackHandler or inputStream is {@code null}
         * @see #build(CsvCallbackHandler, InputStream, Charset)
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final InputStream inputStream)
            throws IOException {
            return build(callbackHandler, inputStream, StandardCharsets.UTF_8);
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified input stream and character set.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered InputStream
         * implementation such as {@link java.io.BufferedInputStream}. Performance may be even likely
         * better if you do not.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, this method will immediately cause consumption of the
         * input stream to read the BOM header and determine the character set.
         * <p>
         * Use {@link #build(CsvCallbackHandler, Path, Charset)} for optimal performance when reading files.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param inputStream     the input stream to read data from.
         * @param charset         the character set to use. If BOM header detection is enabled
         *                        (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                        when no BOM header was found.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if callbackHandler, inputStream or charset is {@code null}
         * @see #build(CsvCallbackHandler, InputStream)
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                      final InputStream inputStream, final Charset charset) throws IOException {

            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(inputStream, "inputStream must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            if (detectBomHeader) {
                final var bomIn = new BomInputStream(inputStream, charset);
                return build(callbackHandler, new InputStreamReader(bomIn, bomIn.getCharset()));
            }

            return build(callbackHandler, new InputStreamReader(inputStream, charset));
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified reader.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered Reader
         * implementation such as {@link java.io.BufferedReader}. Performance may be even likely
         * better if you do not.
         * <p>
         * Use {@link #build(CsvCallbackHandler, Path)} for optimal performance when
         * reading files and {@link #build(CsvCallbackHandler, String)} when reading Strings.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param reader          the data source to read from.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if callbackHandler or reader is {@code null}
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Reader reader) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(reader, "reader must not be null");

            final CsvParser csvParser = new CsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, acceptCharsAfterQuotes, callbackHandler, reader);

            return newReader(callbackHandler, csvParser);
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified String.
         * <p>
         * {@link #detectBomHeader(boolean)} has no effect on this method.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param data            the data to read.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if callbackHandler or data is {@code null}
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final String data) {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(data, "data must not be null");

            final CsvParser csvParser = new CsvParser(fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, acceptCharsAfterQuotes, callbackHandler, data);

            return newReader(callbackHandler, csvParser);
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified file.
         * <p>
         * If {@link #detectBomHeader(boolean)} is enabled, the character set is determined by the BOM header.
         * Per default the character set is {@link StandardCharsets#UTF_8}.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param file            the file to read data from.
         * @return a new CsvReader - never {@code null}. Remember to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if callbackHandler or file is {@code null}
         * @see #build(CsvCallbackHandler, Path, Charset)
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler, final Path file)
            throws IOException {
            return build(callbackHandler, file, StandardCharsets.UTF_8);
        }

        /**
         * Constructs a new callback-based {@link CsvReader} for the specified file and character set.
         *
         * @param <T>             the type of the CSV record.
         * @param callbackHandler the record handler to use. Do not reuse a handler after it has been used!
         * @param file            the file to read data from.
         * @param charset         the character set to use. If BOM header detection is enabled
         *                        (via {@link #detectBomHeader(boolean)}), this acts as a default
         *                        when no BOM header was found.
         * @return a new CsvReader - never {@code null}. Remember to close it!
         * @throws IOException          if an I/O error occurs.
         * @throws NullPointerException if callbackHandler, file or charset is {@code null}
         * @see #build(CsvCallbackHandler, Path)
         */
        public <T> CsvReader<T> build(final CsvCallbackHandler<T> callbackHandler,
                                      final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(callbackHandler, "callbackHandler must not be null");
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            final Reader reader = detectBomHeader
                ? BomUtil.openReader(file, charset)
                : new InputStreamReader(Files.newInputStream(file), charset);

            return build(callbackHandler, reader);
        }

        private <T> CsvReader<T> newReader(final CsvCallbackHandler<T> callbackHandler, final CsvParser csvParser) {
            return new CsvReader<>(csvParser, callbackHandler,
                commentStrategy, skipEmptyLines, ignoreDifferentFieldCount);
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
                .add("acceptCharsAfterQuotes=" + acceptCharsAfterQuotes)
                .toString();
        }

    }

}
