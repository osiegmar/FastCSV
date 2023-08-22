package de.siegmar.fastcsv.reader;

import static de.siegmar.fastcsv.reader.CommentStrategy.NONE;
import static de.siegmar.fastcsv.reader.CommentStrategy.READ;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvIndexTest {

    private final CsvIndex.CsvPage defaultPage = new CsvIndex.CsvPage(0, 1);

    private final List<CsvIndex.CsvPage> defaultPages = List.of(defaultPage);

    private final CsvIndexBuilder defaultBuilder = builder()
        .fileSize(5)
        .fieldSeparator(',')
        .quoteCharacter('"')
        .commentStrategy(NONE)
        .commentCharacter('#')
        .rowCount(2)
        .pages(defaultPages);

    @Test
    void identical() {
        final CsvIndex idx = defaultBuilder.build();
        assertThat(idx)
            .isEqualTo(redundant(idx));
    }

    @Test
    void equal() {
        final CsvIndex idx = defaultBuilder.build();
        assertThat(idx)
            .isEqualTo(defaultBuilder.build());
    }

    @SuppressWarnings("AssertThatObjectIsNullOrNotNull")
    @Test
    void notNull() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(null);
    }

    @Test
    void differentClass() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(new Object());
    }

    @Test
    void fileSize() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.fileSize(6).build());
    }

    @Test
    void fieldSeparator() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.fieldSeparator(';').build());
    }

    @Test
    void quoteCharacter() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.quoteCharacter('\'').build());
    }

    @Test
    void commentStrategy() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.commentStrategy(READ).build());
    }

    @Test
    void commentCharacter() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.commentCharacter(';').build());
    }

    @Test
    void rowCount() {
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.rowCount(3).build());
    }

    @Test
    void additionalPage() {
        final List<CsvIndex.CsvPage> csvPages = List.of(
            new CsvIndex.CsvPage(0, 1),
            new CsvIndex.CsvPage(3, 2)
        );
        assertThat(defaultBuilder.build())
            .isNotEqualTo(defaultBuilder.pages(csvPages).build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void differentPage() {
        final List<CsvIndex.CsvPage> nullPage = new ArrayList<>();
        nullPage.add(null);

        final List wrongTypePage = List.of(new Object());

        assertThat(defaultBuilder.build())
            .isEqualTo(defaultBuilder.pages(List.of(defaultPage)).build())
            .isNotEqualTo(defaultBuilder.pages(
                List.of(new CsvIndex.CsvPage(1, 1))
            ).build())
            .isNotEqualTo(defaultBuilder.pages(
                List.of(new CsvIndex.CsvPage(0, 2))
            ).build())
            .isNotEqualTo(defaultBuilder.pages(nullPage).build())
            .isNotEqualTo(defaultBuilder.pages(wrongTypePage).build());
    }

    private Object redundant(final CsvIndex index) {
        return index;
    }

    private CsvIndexBuilder builder() {
        return new CsvIndexBuilder();
    }

    @SuppressWarnings({"checkstyle:HiddenField", "PMD.AvoidFieldNameMatchingMethodName"})
    static class CsvIndexBuilder {

        private long fileSize;
        private char fieldSeparator;
        private char quoteCharacter;
        private CommentStrategy commentStrategy;
        private char commentCharacter;
        private long rowCount;
        private List<CsvIndex.CsvPage> pages;

        CsvIndexBuilder fileSize(final long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        CsvIndexBuilder fieldSeparator(final char fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        CsvIndexBuilder quoteCharacter(final char quoteCharacter) {
            this.quoteCharacter = quoteCharacter;
            return this;
        }

        CsvIndexBuilder commentStrategy(final CommentStrategy commentStrategy) {
            this.commentStrategy = commentStrategy;
            return this;
        }

        CsvIndexBuilder commentCharacter(final char commentCharacter) {
            this.commentCharacter = commentCharacter;
            return this;
        }

        CsvIndexBuilder rowCount(final long rowCount) {
            this.rowCount = rowCount;
            return this;
        }

        CsvIndexBuilder pages(final List<CsvIndex.CsvPage> pages) {
            this.pages = pages;
            return this;
        }

        CsvIndex build() {
            return new CsvIndex(fileSize, (byte) fieldSeparator, (byte) quoteCharacter,
                commentStrategy, (byte) commentCharacter,
                rowCount, pages);
        }

    }

}
