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
public class CsvReader implements Iterable<CsvRow>, Closeable {

    private final RowReader rowReader;
    private final boolean skipEmptyRows;
    private final boolean errorOnDifferentFieldCount;
    private final CloseableIterator<CsvRow> csvRowIterator = new CsvRowIterator();

    private final Reader reader;
    private final RowHandler rowHandler = new RowHandler(32);
    private long lineNo;
    private int firstLineFieldCount = -1;
    private boolean finished;

    CsvReader(final Reader reader, final char fieldSeparator, final char quoteCharacter,
              final boolean skipEmptyRows, final boolean errorOnDifferentFieldCount) {

        this.reader = reader;
        rowReader = new RowReader(reader, fieldSeparator, quoteCharacter);
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
        return StreamSupport.stream(spliterator(), false);
    }

    private CsvRow fetchRow() throws IOException {
        while (!finished) {
            final long startingLineNo = lineNo + 1;
            finished = rowReader.fetchAndRead(rowHandler);
            final String[] currentFields = rowHandler.end();
            lineNo += rowHandler.getLines();

            final int fieldCount = currentFields.length;

            // reached end of data in a new line?
            if (fieldCount == 0) {
                break;
            }

            // skip empty rows
            if (skipEmptyRows && fieldCount == 1 && currentFields[0].isEmpty()) {
                continue;
            }

            // check the field count consistency on every row
            if (errorOnDifferentFieldCount) {
                if (firstLineFieldCount == -1) {
                    firstLineFieldCount = fieldCount;
                } else if (fieldCount != firstLineFieldCount) {
                    throw new IOException(
                        String.format("Line %d has %d fields, but first line has %d fields",
                            lineNo, fieldCount, firstLineFieldCount));
                }
            }

            return new CsvRowImpl(startingLineNo, currentFields);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private final class CsvRowIterator implements CloseableIterator<CsvRow> {

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
