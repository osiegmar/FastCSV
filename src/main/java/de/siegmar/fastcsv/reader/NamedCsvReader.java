package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Header name based Csv reader implementation.
 * <p>
 * Example use:
 * <pre>{@code
 * try (NamedCsvReader csvReader = NamedCsvReader.builder().build(path)) {
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

    @Override
    public Spliterator<NamedCsvRow> spliterator() {
        return new CsvRowSpliterator<>(iterator());
    }

    /**
     * Creates a new sequential {@code Stream} from this instance.
     * <p>
     * A close handler is registered by this method in order to close the underlying resources.
     * Don't forget to close the returned stream when you're done.
     *
     * @return a new sequential {@code Stream}.
     */
    public Stream<NamedCsvRow> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(() -> {
            try {
                close();
            } catch (final IOException e) {
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
     * This builder is used to create configured instances of {@link NamedCsvReader}. The default
     * configuration of this class complies with RFC 4180.
     * <p>
     * The line delimiter (line-feed, carriage-return or the combination of both) is detected
     * automatically and thus not configurable.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
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
         * Constructs a new {@link NamedCsvReader} for the specified path using UTF-8 as the character set.
         *
         * @param path    the file to read data from.
         * @return a new NamedCsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException if an I/O error occurs.
         * @throws NullPointerException if path or charset is {@code null}
         */
        public NamedCsvReader build(final Path path) throws IOException {
            return build(path, StandardCharsets.UTF_8);
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         *
         * @param path    the file to read data from.
         * @param charset the character set to use.
         * @return a new NamedCsvReader - never {@code null}. Don't forget to close it!
         * @throws IOException if an I/O error occurs.
         * @throws NullPointerException if path or charset is {@code null}
         */
        public NamedCsvReader build(final Path path, final Charset charset) throws IOException {
            return new NamedCsvReader(csvReaderBuilder().build(path, charset));
        }

        /**
         * Constructs a new {@link NamedCsvReader} for the specified arguments.
         * <p>
         * This library uses built-in buffering, so you do not need to pass in a buffered Reader
         * implementation such as {@link java.io.BufferedReader}. Performance may be even likely
         * better if you do not.
         * Use {@link #build(Path, Charset)} for optimal performance when
         * reading files and {@link #build(String)} when reading Strings.
         *
         * @param reader the data source to read from.
         * @return a new NamedCsvReader - never {@code null}.
         * @throws NullPointerException if reader is {@code null}
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

}
