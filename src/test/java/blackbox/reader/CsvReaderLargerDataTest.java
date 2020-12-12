package blackbox.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;

public class CsvReaderLargerDataTest {

    private static final String[] TEXTS = {
        "Lorem ipsum dolor sit amet",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
            + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
            + "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute "
            + "irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
            + "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia "
            + "deserunt mollit anim id est laborum.",
        "Lorem ipsum \"dolor\" sit amet",
        "Lorem ipsum dolor\rsit amet",
        "Lorem ipsum dolor\r\n sit amet",
        "Lorem ipsum dolor\n sit amet",
    };

    @Test
    public void largerData() throws IOException {
        final CsvReader reader = CsvReader.builder().build(new StringReader(createSampleCSV()));
        int i = 0;
        for (final CsvRow row : reader) {
            assertEquals(6, row.getFieldCount());
            assertEquals(TEXTS[0], row.getField(0));
            assertEquals(TEXTS[1], row.getField(1));
            assertEquals(TEXTS[2], row.getField(2));
            assertEquals(TEXTS[3], row.getField(3));
            assertEquals(TEXTS[4], row.getField(4));
            assertEquals(TEXTS[5], row.getField(5));
            i++;
        }
        assertEquals(1000, i);
    }

    private String createSampleCSV() throws IOException {
        final StringWriter sw = new StringWriter();
        final CsvWriter writer = CsvWriter.builder().build(sw);
        for (int i = 0; i < 1000; i++) {
            writer.writeLine(TEXTS);
        }
        return sw.toString();
    }

}
