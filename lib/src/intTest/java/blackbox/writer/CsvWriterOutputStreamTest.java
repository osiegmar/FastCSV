package blackbox.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.writer.CsvWriter;

class CsvWriterOutputStreamTest {

    @Test
    void writeToOutputStream() {
        final var out = new ByteArrayOutputStream();
        CsvWriter.builder().autoFlush(true).build(out)
            .writeRecord("foo", "😎");
        assertThat(out).hasToString("foo,😎\r\n");
    }

}
