package de.siegmar.fastcsv.reader;

import static testutil.CsvRecordAssert.assertThat;

import org.junit.jupiter.api.Test;

class RecordHandlerTest {

    @Test
    void test() {
        final RecordHandler rh = new RecordHandler(1, null);
        rh.add("foo");
        rh.add("bar");

        assertThat(rh.buildAndReset())
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

}
