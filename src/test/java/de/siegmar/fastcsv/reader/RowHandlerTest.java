package de.siegmar.fastcsv.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class RowHandlerTest {

    @Test
    public void test() {
        final RowHandler rh = new RowHandler(1);
        rh.add("foo");
        rh.add("bar");

        assertArrayEquals(new String[]{"foo", "bar"}, rh.endAndReset());
    }

}
