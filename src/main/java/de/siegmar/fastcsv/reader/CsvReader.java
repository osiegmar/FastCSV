package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is the main class for reading CSV data.
 *
 * Obtain via:
 * <pre>{@code
 * CsvReader.builder().build(...)
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
        rowReader = new RowReader(reader, fieldSeparator, quoteCharacter,
            commentStrategy, commentCharacter);
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

    /**
     * Wraps this instance in a {@link NamedCsvReader} instance to allow accessing csv rows
     * via {@link NamedCsvRow}.
     * @return a new {@link NamedCsvReader} instance.
     * @see NamedCsvRow
     */
    public NamedCsvReader withHeader() {
        return new NamedCsvReader(this);
    }

    @Override
    public CloseableIterator<CsvRow> iterator() {
        return csvRowIterator;
    }

    @Override
    public Spliterator<CsvRow> spliterator() {
        return new CsvRowSpliterator<>(csvRowIterator);
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

            // skip empty rows
            if (skipEmptyRows && fieldCount == 1 && currentFields[0].isEmpty()) {
                continue;
            }

            if (isCommentRow) {
                // skip commented rows
                if (commentStrategy == CommentStrategy.SKIP) {
                    continue;
                }
            } else if (errorOnDifferentFieldCount) {
                // check the field count consistency on every row
                if (firstLineFieldCount == -1) {
                    firstLineFieldCount = fieldCount;
                } else if (fieldCount != firstLineFieldCount) {
                    throw new IOException(
                        String.format("Row %d has %d fields, but first row had %d fields",
                            startingLineNo, fieldCount, firstLineFieldCount));
                }
            }

            return new CsvRowImpl(startingLineNo, currentFields, isCommentRow);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
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

}
