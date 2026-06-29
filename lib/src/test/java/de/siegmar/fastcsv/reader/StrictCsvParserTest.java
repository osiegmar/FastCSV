package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class StrictCsvParserTest {

    @Test
    void nonPositiveMaxBufferSize() {
        // The builder guards against this, but the parser's own buffer contract must hold too.
        assertThatThrownBy(() -> new StrictCsvParser(',', '"', CommentStrategy.NONE, '#', false, true,
            CsvRecordHandler.of(), 0, new StringReader("foo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxBufferSize must be > 0");
    }

}
