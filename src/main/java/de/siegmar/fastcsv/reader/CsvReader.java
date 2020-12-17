package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is the main class for reading CSV data.
 * <p>
 * Example use:
 * <pre>{@code
 * try (CsvReader csvReader = CsvReader.builder().build(path, charset)) {
 *     for (CsvRow row : csvReader) {
 *         ...
 *     }
 * }
 * }</pre>
 */
public final class CsvReader implements Iterable<CsvRow>, Closeable {

    private final RowReader rowReader;
    private final CommentStrategy commentStrategy;
    private final boolean skipEmptyRows;
    private final boolean errorOnDifferentFieldCount;
    private final CloseableIterator<CsvRow> csvRowIterator = new CsvRowIterator();

    private final Reader reader;
    private final RowHandler rowHandler = new RowHandler(32);
    private long lineNo;
    private int firstLineFieldCount = -1;
    private boolean finished;

    CsvReader(final Reader reader, final char fieldSeparator, final char quoteCharacter,
              final CommentStrategy commentStrategy, final char commentCharacter,
              final boolean skipEmptyRows, final boolean errorOnDifferentFieldCount) {

        this.reader = reader;
        rowReader = new RowReader(reader, fieldSeparator, quoteCharacter, commentStrategy,
            commentCharacter);
        this.commentStrategy = commentStrategy;
        this.skipEmptyRows = skipEmptyRows;
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
    }

    /**
     * Constructs a {@link CsvReaderBuilder} to configure and build instances of this class.
     * @return a new {@link CsvReaderBuilder} instance.
     */
    public static CsvReaderBuilder builder() {
        return new CsvReaderBuilder();
    }

    @Override
    public CloseableIterator<CsvRow> iterator() {
        return csvRowIterator;
    }

    @Override
    public Spliterator<CsvRow> spliterator() {
        return new CsvRowSpliterator(csvRowIterator);
    }

    /**
     * Creates a new sequential {@code Stream} from this instance.
     *
     * @return a new sequential {@code Stream}.
     */
    public Stream<CsvRow> stream() {
        return StreamSupport.stream(spliterator(), false)
            .onClose(() -> {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private CsvRow fetchRow() throws IOException {
        while (!finished) {
            final long startingLineNo = lineNo + 1;
            finished = rowReader.fetchAndRead(rowHandler);
            final boolean isCommentRow = rowHandler.isCommentMode();
            lineNo += rowHandler.getLines();
            final String[] currentFields = rowHandler.endAndReset();

            final int fieldCount = currentFields.length;

            // reached end of data in a new line?
            if (fieldCount == 0) {
                break;
            }

            // skip commented rows
            if (isCommentRow && commentStrategy == CommentStrategy.SKIP) {
                continue;
            }

            // skip empty rows
            if (fieldCount == 1 && currentFields[0].isEmpty()) {
                if (skipEmptyRows) {
                    continue;
                } else {
                    return new CsvRow(startingLineNo);
                }
            }

            if (errorOnDifferentFieldCount) {
                // check the field count consistency on every row
                if (firstLineFieldCount == -1) {
                    firstLineFieldCount = fieldCount;
                } else if (fieldCount != firstLineFieldCount) {
                    throw new IOException(
                        String.format("Row %d has %d fields, but first row had %d fields",
                            startingLineNo, fieldCount, firstLineFieldCount));
                }
            }

            return new CsvRow(startingLineNo, currentFields, isCommentRow);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvReader.class.getSimpleName() + "[", "]")
            .add("commentStrategy=" + commentStrategy)
            .add("skipEmptyRows=" + skipEmptyRows)
            .add("errorOnDifferentFieldCount=" + errorOnDifferentFieldCount)
            .toString();
    }

    private class CsvRowIterator implements CloseableIterator<CsvRow> {

        private CsvRow fetchedRow;
        private boolean fetched;

        @Override
        public boolean hasNext() {
            if (!fetched) {
                fetch();
            }
            return fetchedRow != null;
        }

        @Override
        public CsvRow next() {
            if (!fetched) {
                fetch();
            }
            if (fetchedRow == null) {
                throw new NoSuchElementException();
            }
            fetched = false;

            return fetchedRow;
        }

        private void fetch() {
            try {
                fetchedRow = fetchRow();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            fetched = true;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    /**
     * This builder is used to create configured instances of {@link CsvReader}. The default
     * configuration of this class complies with RFC 4180.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    public static final class CsvReaderBuilder {

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private CommentStrategy commentStrategy = CommentStrategy.NONE;
        private char commentCharacter = '#';
        private boolean skipEmptyRows = true;
        private boolean errorOnDifferentFieldCount;

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
         * Defines if empty rows should be skipped when reading data.
         *
         * @param skipEmptyRows if empty rows should be skipped (default: {@code true}).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvReaderBuilder skipEmptyRows(final boolean skipEmptyRows) {
            this.skipEmptyRows = skipEmptyRows;
            return this;
        }

        /**
         * Defines if an error should be thrown if lines do contain a different number of columns.
         *
         * @param errorOnDifferentFieldCount if an exception should be thrown, if CSV data contains
         *                                   different field count (default: {@code false}).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public CsvReaderBuilder errorOnDifferentFieldCount(
            final boolean errorOnDifferentFieldCount) {
            this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
            return this;
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param path    the file to read data from.
         * @param charset the character set to use - must not be {@code null}.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException if an I/O error occurs.
         */
        public CsvReader build(final Path path, final Charset charset) throws IOException {
            Objects.requireNonNull(path, "path must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return newReader(new InputStreamReader(Files.newInputStream(path), charset));
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use - must not be {@code null}.
         * @return a new CsvReader - never {@code null}.
         * @throws IOException if an I/O error occurs.
         */
        public CsvReader build(final File file, final Charset charset) throws IOException {
            Objects.requireNonNull(file, "file must not be null");
            Objects.requireNonNull(charset, "charset must not be null");

            return newReader(new InputStreamReader(new FileInputStream(file), charset));
        }

        /**
         * Constructs a new {@link CsvReader} for the specified arguments.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered Reader
         * implementation such as {@link java.io.BufferedReader}.
         * Performance may be even likely better if you do not.
         *
         * @param reader the data source to read from.
         * @return a new CsvReader - never {@code null}.
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
            return newReader(
                new StringReader(Objects.requireNonNull(data, "data must not be null")));
        }

        private CsvReader newReader(final Reader reader) {
            return new CsvReader(reader, fieldSeparator, quoteCharacter, commentStrategy,
                commentCharacter, skipEmptyRows, errorOnDifferentFieldCount);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CsvReaderBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentStrategy=" + commentStrategy)
                .add("commentCharacter=" + commentCharacter)
                .add("skipEmptyRows=" + skipEmptyRows)
                .add("errorOnDifferentFieldCount=" + errorOnDifferentFieldCount)
                .toString();
        }

    }

    static final class CsvRowSpliterator implements Spliterator<CsvRow> {

        private static final int CHARACTERISTICS = ORDERED | DISTINCT | NONNULL | IMMUTABLE;

        private final Iterator<CsvRow> iterator;

        CsvRowSpliterator(final Iterator<CsvRow> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super CsvRow> action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.next());
            return true;
        }

        @Override
        public void forEachRemaining(final Consumer<? super CsvRow> action) {
            iterator.forEachRemaining(action);
        }

        @Override
        public Spliterator<CsvRow> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return CHARACTERISTICS;
        }

    }

}
