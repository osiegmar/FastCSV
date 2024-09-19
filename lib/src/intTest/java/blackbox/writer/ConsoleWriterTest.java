package blackbox.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.writer.CsvWriter;

class ConsoleWriterTest {

    @SuppressWarnings("checkstyle:RegexpMultiline")
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    void console() {
        CsvWriter.builder().toConsole()
            .writeRecord("foo", "bar");

        assertThat(capturedOut).asString()
            .isEqualTo("foo,bar\r\n");
    }

}
