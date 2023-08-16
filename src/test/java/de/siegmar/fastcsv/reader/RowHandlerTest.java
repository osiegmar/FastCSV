package de.siegmar.fastcsv.reader;

import static testutil.CsvRowAssert.assertThat;

import org.junit.jupiter.api.Test;

class RowHandlerTest {

    @Test
    void test() {
        final RowHandler rh = new RowHandler(1);
        rh.add("foo");
        rh.add("bar");

        assertThat(rh.buildAndReset())
            .isOriginalLineNumber(1)
            .isNotComment()
            .fields().containsExactly("foo", "bar");
    }

}
