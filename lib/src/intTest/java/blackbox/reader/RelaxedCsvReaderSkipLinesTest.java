package blackbox.reader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
class RelaxedCsvReaderSkipLinesTest extends AbstractSkipLinesTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("de.siegmar.fastcsv.relaxed", "true");
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("de.siegmar.fastcsv.relaxed");
    }

}
