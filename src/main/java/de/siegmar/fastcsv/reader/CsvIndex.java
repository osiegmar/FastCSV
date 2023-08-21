package de.siegmar.fastcsv.reader;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Index built by {@link IndexedCsvReader} to access large CSV data files.
 */
public final class CsvIndex implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long fileSize;
    private final byte fieldSeparator;
    private final byte quoteCharacter;
    private final CommentStrategy commentStrategy;
    private final byte commentCharacter;
    private final List<CsvPage> pages;
    private final long rowCounter;

    CsvIndex(final long fileSize, final byte fieldSeparator, final byte quoteCharacter,
             final CommentStrategy commentStrategy, final byte commentCharacter,
             final List<CsvPage> pages, final long rowCounter) {
        this.fileSize = fileSize;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
        this.commentStrategy = commentStrategy;
        this.commentCharacter = commentCharacter;
        this.pages = pages;
        this.rowCounter = rowCounter;
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

    /**
     * Gets the number of pages the file contents is partitioned to.
     *
     * @return the number of pages the file contents is partitioned to
     */
    public int pageCount() {
        return pages.size();
    }

    /**
     * Gets the number of rows the file contains.
     *
     * @return the number of rows the file contains
     */
    public long rowCount() {
        return rowCounter;
    }

    CsvPage page(final int pageNumber) {
        return pages.get(pageNumber);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CsvIndex csvIndex = (CsvIndex) o;
        return fileSize == csvIndex.fileSize
            && fieldSeparator == csvIndex.fieldSeparator
            && quoteCharacter == csvIndex.quoteCharacter
            && commentCharacter == csvIndex.commentCharacter
            && rowCounter == csvIndex.rowCounter
            && commentStrategy == csvIndex.commentStrategy
            && Objects.equals(pages, csvIndex.pages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileSize, fieldSeparator, quoteCharacter, commentStrategy, commentCharacter,
            pages, rowCounter);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CsvIndex.class.getSimpleName() + "[", "]")
            .add("fileSize=" + fileSize)
            .add("fieldSeparator=" + fieldSeparator)
            .add("quoteCharacter=" + quoteCharacter)
            .add("commentStrategy=" + commentStrategy)
            .add("commentCharacter=" + commentCharacter)
            .add("rowCounter=" + rowCounter)
            .toString();
    }

}
