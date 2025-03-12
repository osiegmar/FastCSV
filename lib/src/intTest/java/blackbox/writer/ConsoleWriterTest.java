package blackbox.writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import de.siegmar.fastcsv.writer.CsvWriter;

class ConsoleWriterTest {

    @EnabledIf("isConsoleAvailable")
    @Test
    void console() {
        CsvWriter.builder().toConsole()
            .writeRecord("ConsoleWriterTest");
    }

    private boolean isConsoleAvailable() {
        return System.console() != null;
    }

}
