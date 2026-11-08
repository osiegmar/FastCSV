package de.siegmar.fastcsv.reader;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/// Index built by [IndexedCsvReader] to access large CSV data files.
///
/// Instances of this class are immutable and safe for use by multiple threads.
///
/// The index holds one [CsvPage] per [#pageSize()] records, so its memory footprint grows with the record
/// count, not with the file size.
///
/// This class implements [Serializable], so an index can be persisted and reloaded
/// to avoid re-indexing the same file. A previously built index can be reused by passing it to
/// [IndexedCsvReader.IndexedCsvReaderBuilder#index(CsvIndex)], whose documentation describes what has to match.
///
/// Although the constructor is public (mandatory for record classes),
/// this class is **not intended to be instantiated directly.**
///
/// @param bomHeaderLength  The length of an optional BOM, in bytes
/// @param fileSize         The size in bytes of the CSV file this index was built for
/// @param fieldSeparator   The field separator used when building this index
/// @param quoteCharacter   The quote character used when building this index
/// @param commentStrategy  The comment strategy used when building this index
/// @param commentCharacter The comment character used when building this index
/// @param pageSize         The page size the pages of this index were partitioned with
/// @param recordCount      The total number of records in the CSV file, including empty lines, comments and a header
/// @param pages            The pages this index is partitioned into
public record CsvIndex(int bomHeaderLength, long fileSize, byte fieldSeparator, byte quoteCharacter,
                       CommentStrategy commentStrategy, byte commentCharacter, int pageSize, long recordCount,
                       List<CsvPage> pages) implements Serializable {

    /// Constructor for the [CsvIndex] class.
    ///
    /// @throws NullPointerException if `commentStrategy` or `pages` is `null`
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CsvIndex(final int bomHeaderLength, final long fileSize, final byte fieldSeparator,
                    final byte quoteCharacter, final CommentStrategy commentStrategy, final byte commentCharacter,
                    final int pageSize, final long recordCount, final List<CsvPage> pages) {
        this.bomHeaderLength = bomHeaderLength;
        this.fileSize = fileSize;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = Objects.requireNonNull(commentStrategy, "commentStrategy must not be null");
        this.commentCharacter = commentCharacter;
        this.pageSize = pageSize;
        this.recordCount = recordCount;
        this.pages = List.copyOf(Objects.requireNonNull(pages, "pages must not be null"));
    }

    /// {@return string representation of this index without the pages themselves}
    @Override
    public String toString() {
        return new StringJoiner(", ", CsvIndex.class.getSimpleName() + "[", "]")
            .add("bomHeaderLength=" + bomHeaderLength)
            .add("fileSize=" + fileSize)
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .add("pageSize=" + pageSize)
            .add("recordCount=" + recordCount)
            .add("pageCount=" + pages.size())
            .toString();
    }

    /// Represents a page of the CSV file.
    ///
    /// @param offset             The offset in bytes of the page from the start of the CSV file (including a BOM)
    /// @param startingLineNumber The starting line number of the first record of the page
    public record CsvPage(long offset, long startingLineNumber) implements Serializable {
    }

}
