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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.siegmar.fastcsv.util.Preconditions;
import de.siegmar.fastcsv.util.Util;

/**
 * This is the main class for reading CSV data.
 * <p>
 * Example use:
 * {@snippet :
 * try (CsvReader csv = CsvReader.builder().build(file)) {
 *     for (CsvRecord csvRecord : csv) {
 *         // ...
 *     }
 * }
 *}
 */
public final class CsvReader implements Iterable<CsvRecord>, Closeable {

    private final RecordReader recordReader;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyLines;
    private final boolean ignoreDifferentFieldCount;
    private final CloseableIterator<CsvRecord> csvRecordIterator = new CsvRecordIterator();

    private int firstRecordFieldCount = -1;

    CsvReader(final Reader reader, final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean skipEmptyLines, final boolean ignoreDifferentFieldCount) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;

        recordReader = new RecordReader(reader, fieldSeparator, quoteCharacter, commentStrategy,
            commentCharacter);
    }

    @SuppressWarnings("PMD.NullAssignment")
    CsvReader(final String data, final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean skipEmptyLines, final boolean ignoreDifferentFieldCount) {

        assertFields(fieldSeparator, quoteCharacter, commentCharacter);

        this.commentStrategy = commentStrategy;
        this.skipEmptyLines = skipEmptyLines;
        this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;

        recordReader = new RecordReader(data, fieldSeparator, quoteCharacter, commentStrategy,
            commentCharacter);
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
     * @return a new {@link CsvReaderBuilder} instance.
     */
    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    @Override
    public CloseableIterator<CsvRecord> iterator() {
        return csvRecordIterator;
    }

    @Override
    public Spliterator<CsvRecord> spliterator() {
        return new CsvRecordSpliterator<>(csvRecordIterator);
    }

    /**
     * Creates a new sequential {@link Stream} from this instance.
     * <p>
     * A close handler is registered by this method in order to close the underlying resources.
     * Don't forget to close the returned stream when you're done.
     *
     * @return a new sequential {@link Stream}.
     */
    public Stream<CsvRecord> stream() {
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
    private CsvRecord fetchRow() throws IOException {
        CsvRecord csvRecord;
        while ((csvRecord = recordReader.fetchAndRead()) != null) {
            // skip commented records
            if (commentStrategy == CommentStrategy.SKIP && csvRecord.comment()) {
                continue;
            }

            // skip empty records
            if (csvRecord.empty()) {
                if (skipEmptyLines) {
                    continue;
                }
            } else if (!ignoreDifferentFieldCount) {
                final int fieldCount = csvRecord.fieldCount();

                // check the field count consistency on every record
                if (firstRecordFieldCount == -1) {
                    firstRecordFieldCount = fieldCount;
                } else if (fieldCount != firstRecordFieldCount) {
                    throw new MalformedCsvException(
                        String.format("Record %d has %d fields, but first record had %d fields",
                            csvRecord.originalLineNumber(), fieldCount, firstRecordFieldCount));
                }
            }

            break;
        }

        return csvRecord;
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

    private class CsvRecordIterator implements CloseableIterator<CsvRecord> {

        private CsvRecord fetchedRecord;
        private boolean fetched;

        @Override
        public boolean hasNext() {
            if (!fetched) {
                fetch();
            }
            return fetchedRecord != null;
        }

        @Override
        public CsvRecord next() {
            if (!fetched) {
                fetch();
            }
            if (fetchedRecord == null) {
                throw new NoSuchElementException();
            }
            fetched = false;

            return fetchedRecord;
        }

        private void fetch() {
            try {
                fetchedRecord = fetchRow();
            } catch (final IOException e) {
                if (fetchedRecord != null) {
                    throw new UncheckedIOException("IOException when reading record that started in line "
                        + (fetchedRecord.originalLineNumber() + 1), e);
                } else {
                    throw new UncheckedIOException("IOException when reading first record", e);
                }
            }
            fetched = true;
        }

        @Override
        public void close() throws IOException {
            CsvReader.this.close();
        }

    }

    /**
     * This builder is used to create configured instances of {@link CsvReader}. The default
     * configuration of this class complies with RFC 4180.
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

        private CsvReaderBuilder() {
        }

        /**
         * Sets the {@code fieldSeparator} used when reading CSV data.
         *
         * @param fieldSeparator the field separator character (default: {@code ,} - comma).
         * @return This updated object, so that additional method calls can be chained together.
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
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the strategy that defines how (and if) commented lines should be handled
         * (default: {@link CommentStrategy#NONE} as comments are not defined in RFC 4180).
         *
         * @param commentStrategy the strategy for handling comments.
         * @return This updated object, so that additional method calls can be chained together.
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
         * @return This updated object, so that additional method calls can be chained together.
         * @see #commentStrategy(CommentStrategy)
         */
        public CsvReaderBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Defines if empty records should be skipped when reading data.
         *
         * @param skipEmptyLines if empty records should be skipped (default: {@code true}).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvReaderBuilder skipEmptyLines(final boolean skipEmptyLines) {
            this.skipEmptyLines = skipEmptyLines;
            return this;
        }

        /**
         * Defines if an {@link MalformedCsvException} should be thrown if records do contain a
         * different number of fields.
         *
         * @param ignoreDifferentFieldCount if exception should be suppressed, when CSV data contains
         *                                   different field count (default: {@code true}).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvReaderBuilder ignoreDifferentFieldCount(final boolean ignoreDifferentFieldCount) {
            this.ignoreDifferentFieldCount = ignoreDifferentFieldCount;
            return this;
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
         * @param reader the data source to read from.
         * @return a new CsvReader - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
         */
        public CsvReader build(final Reader reader) {
            return newReader(Objects.requireNonNull(reader, "reader must not be null"));
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param data    the data to read.
         * @return a new CsvReader - never {@code null}.
         */
        public CsvReader build(final String data) {
            return newReader(Objects.requireNonNull(data, "data must not be null"));
        }

        /**
         * Constructs a new {@link CsvReader} for the specified file using UTF-8 as the character set.
         *
         * @param file    the file to read data from.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvReader build(final Path file) throws IOException {
            return build(file, StandardCharsets.UTF_8);
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use.
         * @return a new CsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException if an I/O error occurs.
         * @throws NullPointerException if file or charset is {@code null}
         */
        public CsvReader build(final Path file, final Charset charset) throws IOException {
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return newReader(new InputStreamReader(Files.newInputStream(file), charset));
        }

        private CsvReader newReader(final Reader reader) {
            return new CsvReader(reader, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, skipEmptyLines, ignoreDifferentFieldCount);
        }

        private CsvReader newReader(final String data) {
            return new CsvReader(data, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, skipEmptyLines, ignoreDifferentFieldCount);
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
                .toString();
        }

    }

}
