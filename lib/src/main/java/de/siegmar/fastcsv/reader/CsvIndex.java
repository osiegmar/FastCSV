package de.siegmar.fastcsv.reader;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/// Index built by [IndexedCsvReader] to access large CSV data files.
public final class CsvIndex implements Serializable {

    private static final long serialVersionUID = 1L;

    /// The length of an optional BOM header.
    private final int bomHeaderLength;

    /// The CSV file size this index was built for.
    private final long fileSize;

    /// The field separator used when building this index.
    private final byte fieldSeparator;

    /// The quote character used when building this index.
    private final byte quoteCharacter;

    /// The comment strategy used when building this index.
    private final CommentStrategy commentStrategy;

    /// The comment character used when building this index.
    private final byte commentCharacter;

    /// The total number of records the CSV file contains this index was built for.
    private final long recordCounter;

    /// The pages this index is partitioned.
    @SuppressWarnings("serial")
    private final List<CsvPage> pages;

    @SuppressWarnings("checkstyle:ParameterNumber")
    CsvIndex(final int bomHeaderLength, final long fileSize, final byte fieldSeparator, final byte quoteCharacter,
             final CommentStrategy commentStrategy, final byte commentCharacter,
             final long recordCounter, final List<CsvPage> pages) {
        this.bomHeaderLength = bomHeaderLength;
        this.fileSize = fileSize;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = Objects.requireNonNull(commentStrategy);
        this.commentCharacter = commentCharacter;
        this.recordCounter = recordCounter;
        this.pages = Objects.requireNonNull(pages);
    }

    int getBomHeaderLength() {
        return bomHeaderLength;
    }

    long getFileSize() {
        return fileSize;
    }

    byte getFieldSeparator() {
        return fieldSeparator;
    }

    byte getQuoteCharacter() {
        return quoteCharacter;
    }

    CommentStrategy getCommentStrategy() {
        return commentStrategy;
    }

    byte getCommentCharacter() {
        return commentCharacter;
    }

    /// Gets the number of pages the file contents is partitioned to.
    ///
    /// @return the number of pages the file contents is partitioned to
    public int getPageCount() {
        return pages.size();
    }

    /// Gets the number of records the file contains.
    ///
    /// @return the number of records the file contains
    public long getRecordCount() {
        return recordCounter;
    }

    CsvPage getPage(final int pageNumber) {
        return pages.get(pageNumber);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CsvIndex csvIndex = (CsvIndex) o;
        return bomHeaderLength == csvIndex.bomHeaderLength
            && fileSize == csvIndex.fileSize
            && fieldSeparator == csvIndex.fieldSeparator
            && quoteCharacter == csvIndex.quoteCharacter
            && commentStrategy == csvIndex.commentStrategy
            && commentCharacter == csvIndex.commentCharacter
            && recordCounter == csvIndex.recordCounter
            && Objects.equals(pages, csvIndex.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bomHeaderLength, fileSize, fieldSeparator, quoteCharacter, commentStrategy,
            commentCharacter, pages, recordCounter);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvIndex.class.getSimpleName() + "[", "]")
            .add("bomHeaderLength=" + bomHeaderLength)
            .add("fileSize=" + fileSize)
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .add("recordCount=" + recordCounter)
            .add("pageCount=" + pages.size())
            .toString();
    }

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    static final class CsvPage implements Serializable {

        private static final long serialVersionUID = 1L;

        private final long offset;
        private final long startingLineNumber;

        CsvPage(final long offset, final long startingLineNumber) {
            this.offset = offset;
            this.startingLineNumber = startingLineNumber;
        }

        long getOffset() {
            return offset;
        }

        long getStartingLineNumber() {
            return startingLineNumber;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final CsvPage csvPage = (CsvPage) o;
            return offset == csvPage.offset
                && startingLineNumber == csvPage.startingLineNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, startingLineNumber);
        }

    }

}
