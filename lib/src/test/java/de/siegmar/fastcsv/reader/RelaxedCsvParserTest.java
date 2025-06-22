package de.siegmar.fastcsv.reader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RelaxedCsvParserTest {

    private final RelaxedCsvParser parser =
        new RelaxedCsvParser(",", '"', CommentStrategy.NONE, '#', true, CsvRecordHandler.of(), 1024, "foo");

    @Test
    void unsupportedReset() {
        assertThatThrownBy(() -> parser.reset(0))
            .isInstanceOf(UnsupportedOperationException.class);
    }

}
