package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Header name based Csv reader implementation.
 * <p>
 * Example use:
 * <pre>{@code
 * try (NamedCsvReader csvReader = NamedCsvReader.builder().build(path, charset)) {
 *     for (NamedCsvRow row : csvReader) {
 *         ...
 *     }
 * }
 * }</pre>
 */
public final class NamedCsvReader implements Iterable<NamedCsvRow>, Closeable {

    private final CsvReader csvReader;
    private final CloseableIterator<CsvRow> csvIterator;
    private final CloseableIterator<NamedCsvRow> namedCsvIterator;

    private Set<String> header;
    private boolean isInitialized;

    private NamedCsvReader(final CsvReader csvReader) {
        this.csvReader = csvReader;
        csvIterator = csvReader.iterator();
        namedCsvIterator = new NamedCsvRowIterator(csvIterator);
    }

    private void initialize() {
        if (!csvIterator.hasNext()) {
            header = Collections.emptySet();
        } else {
            final CsvRow firstRow = csvIterator.next();

            final Set<String> headerSet = new LinkedHashSet<>(firstRow.getFieldCount());
            for (final String field : firstRow.getFields()) {
                if (!headerSet.add(field)) {
                    throw new IllegalStateException("Duplicate header field '" + field + "' found");
                }
            }
            header = Collections.unmodifiableSet(headerSet);
        }

        isInitialized = true;
    }

    /**
     * Constructs a {@link NamedCsvReaderBuilder} to configure and build instances of this class.
     * @return a new {@link NamedCsvReaderBuilder} instance.
     */
    public static NamedCsvReaderBuilder builder() {
        return new NamedCsvReaderBuilder();
    }

    /**
     * Returns the header columns. Can be called at any time.
     *
     * @return the header columns
     */
    public Set<String> getHeader() {
        if (!isInitialized) {
            initialize();
        }
        return header;
    }

    @Override
    public CloseableIterator<NamedCsvRow> iterator() {
        if (!isInitialized) {
            initialize();
        }

        return namedCsvIterator;
    }

    /**
     * Constructs and returns a new {@link CsvReader.CsvRowSpliterator}.
     *
     * @return a new {@link CsvReader.CsvRowSpliterator} instance
     */
    @Override
    public Spliterator<NamedCsvRow> spliterator() {
        return new NamedCsvRowSpliterator(iterator());
    }

    /**
     * Creates a new sequential {@code Stream} from this instance.
     *
     * @return a new sequential {@code Stream}.
     */
    public Stream<NamedCsvRow> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(() -> {
            try {
                close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NamedCsvReader.class.getSimpleName() + "[", "]")
            .add("header=" + header)
            .add("csvReader=" + csvReader)
            .toString();
    }

    private class NamedCsvRowIterator implements CloseableIterator<NamedCsvRow> {

        private final CloseableIterator<CsvRow> csvIterator;

        NamedCsvRowIterator(final CloseableIterator<CsvRow> csvIterator) {
            this.csvIterator = csvIterator;
        }

        @Override
        public boolean hasNext() {
            return csvIterator.hasNext();
        }

        @Override
        public NamedCsvRow next() {
            return new NamedCsvRow(header, csvIterator.next());
        }

        @Override
        public void close() throws IOException {
            csvIterator.close();
        }

    }

    /**
     * This builder is used to create configured instances of {@link NamedCsvReader}.
     */
    @SuppressWarnings("checkstyle:HiddenField")
    public static final class NamedCsvReaderBuilder {

        private char fieldSeparator = ',';
        private char quoteCharacter = '"';
        private char commentCharacter = '#';
        private boolean skipComments;

        private NamedCsvReaderBuilder() {
        }

        /**
         * Sets the {@code fieldSeparator} used when reading CSV data.
         *
         * @param fieldSeparator the field separator character (default: {@code ,} - comma).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public NamedCsvReaderBuilder fieldSeparator(final char fieldSeparator) {
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
        public NamedCsvReaderBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        /**
         * Sets the {@code commentCharacter} used to comment lines.
         *
         * @param commentCharacter the character used to comment lines (default: {@code #} - hash)
         * @return This updated object, so that additional method calls can be chained together.
         * @see #skipComments(boolean)
         */
        public NamedCsvReaderBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        /**
         * Defines if commented rows should be detected and skipped when reading data.
         *
         * @param skipComments if commented rows should be skipped (default: {@code true}).
         * @return This updated object, so that additional method calls can be chained together.
         */
        public NamedCsvReaderBuilder skipComments(final boolean skipComments) {
            this.skipComments = skipComments;
            return this;
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         *
         * @param path    the file to read data from.
         * @param charset the character set to use - must not be {@code null}.
         * @return a new NamedCsvReader - never {@code null}.
         * @throws IOException if an I/O error occurs.
         */
        public NamedCsvReader build(final Path path, final Charset charset) throws IOException {
            return new NamedCsvReader(csvReaderBuilder().build(path, charset));
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         *
         * @param file    the file to read data from.
         * @param charset the character set to use - must not be {@code null}.
         * @return a new NamedCsvReader - never {@code null}.
         * @throws IOException if an I/O error occurs.
         */
        public NamedCsvReader build(final File file, final Charset charset) throws IOException {
            return new NamedCsvReader(csvReaderBuilder().build(file, charset));
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered Reader
         * implementation such as {@link java.io.BufferedReader}.
         * Performance may be even likely better if you do not.
         *
         * @param reader the data source to read from.
         * @return a new NamedCsvReader - never {@code null}.
         */
        public NamedCsvReader build(final Reader reader) {
            return new NamedCsvReader(csvReaderBuilder().build(reader));
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         *
         * @param data    the data to read.
         * @return a new NamedCsvReader - never {@code null}.
         */
        public NamedCsvReader build(final String data) {
            return new NamedCsvReader(csvReaderBuilder().build(data));
        }

        private CsvReader.CsvReaderBuilder csvReaderBuilder() {
            return CsvReader.builder()
                .fieldSeparator(fieldSeparator)
                .quoteCharacter(quoteCharacter)
                .commentCharacter(commentCharacter)
                .commentStrategy(skipComments ? CommentStrategy.SKIP : CommentStrategy.NONE)
                .errorOnDifferentFieldCount(true);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", NamedCsvReaderBuilder.class.getSimpleName() + "[", "]")
                .add("fieldSeparator=" + fieldSeparator)
                .add("quoteCharacter=" + quoteCharacter)
                .add("commentCharacter=" + commentCharacter)
                .add("skipComments=" + skipComments)
                .toString();
        }

    }

    private static final class NamedCsvRowSpliterator implements Spliterator<NamedCsvRow> {

        private static final int CHARACTERISTICS = ORDERED | DISTINCT | NONNULL | IMMUTABLE;

        private final Iterator<NamedCsvRow> iterator;

        NamedCsvRowSpliterator(final Iterator<NamedCsvRow> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super NamedCsvRow> action) {
            if (!iterator.hasNext()) {
                return false;
            }

            action.accept(iterator.next());
            return true;
        }

        @Override
        public void forEachRemaining(final Consumer<? super NamedCsvRow> action) {
            iterator.forEachRemaining(action);
        }

        @Override
        public Spliterator<NamedCsvRow> trySplit() {
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
