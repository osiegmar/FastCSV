package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static testutil.CsvRecordAssert.assertThat;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

@SuppressWarnings("PMD.CloseResource")
class CsvReaderLargerDataTest {

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

    private static final int TEST_RECORDS = 1000;

    @Test
    void largerData() {
        final CsvReader<CsvRecord> reader = CsvReader.builder()
            .ofCsvRecord(new StringReader(createSampleCSV()));

        assertThat(reader.stream())
            .hasSize(TEST_RECORDS)
            .allSatisfy(csvRecord -> assertThat(csvRecord)
                .isNotComment()
                .fields().containsExactly(TEXTS));
    }

    private String createSampleCSV() {
        final StringWriter sw = new StringWriter();
        final CsvWriter writer = CsvWriter.builder()
            .bufferSize(0)
            .build(sw);
        for (int i = 0; i < TEST_RECORDS; i++) {
            writer.writeRecord(TEXTS);
        }
        return sw.toString();
    }

}
