package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class RelaxedCsvParserTest {

    private final RelaxedCsvParser parser =
        new RelaxedCsvParser(",", '"', CommentStrategy.NONE, '#', true, true,
            CsvRecordHandler.of(), 1024, "foo");

    @Test
    void unsupportedReset() {
        assertThatThrownBy(() -> parser.reset(0))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("PMD.CloseResource")
    @Test
    void peekLineExceedingMaxBufferSize() {
        // A single line longer than maxBufferSize cannot be buffered for peekLine() to inspect.
        final int maxBufferSize = 16;
        final RelaxedCsvParser p = new RelaxedCsvParser(",", '"', CommentStrategy.NONE, '#', true, true,
            CsvRecordHandler.of(), maxBufferSize, new StringReader("X".repeat(maxBufferSize * 4)));

        assertThatThrownBy(p::peekLine)
            .isInstanceOf(CsvParseException.class)
            .hasMessage("The maximum buffer size of %d is insufficient to read a single line."
                .formatted(maxBufferSize));
    }

}
