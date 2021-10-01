package de.siegmar.fastcsv.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class RowHandlerTest {

    @Test
    public void test() {
        final RowHandler rh = new RowHandler(1);
        rh.add("foo", 0);
        rh.add("bar", 4);
        final CsvRow csvRow = rh.buildAndReset();

        assertNotNull(csvRow);
        assertEquals(
            "CsvRow[originalLineNumber=1, startingOffset=0, fields=[foo, bar], comment=false]",
            csvRow.toString());
    }

}
