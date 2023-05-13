package de.siegmar.fastcsv.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class RowHandlerTest {

    @Test
    void test() {
        final RowHandler rh = new RowHandler(1);
        rh.add("foo");
        rh.add("bar");
        final CsvRow csvRow = rh.buildAndReset();

        assertNotNull(csvRow);
        assertEquals("CsvRow[originalLineNumber=1, fields=[foo, bar], comment=false]",
            csvRow.toString());
    }

}
