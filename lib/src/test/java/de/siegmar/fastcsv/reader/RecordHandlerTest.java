package de.siegmar.fastcsv.reader;

import static testutil.CsvRecordAssert.assertThat;

import org.junit.jupiter.api.Test;

class RecordHandlerTest {

    @Test
    void test() {
        final RecordHandler rh = new RecordHandler(1, null);
        rh.add("foo", false);
        rh.add("bar", false);

        assertThat(rh.buildAndReset())
            .isStartingLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

}
