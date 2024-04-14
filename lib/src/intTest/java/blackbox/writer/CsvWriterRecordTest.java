package blackbox.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

class CsvWriterRecordTest {

    private final CsvWriter.CsvWriterBuilder crw = CsvWriter.builder()
        .lineDelimiter(LineDelimiter.LF);

    @Test
    void emptyRecord() throws IOException {
        final var sw = new StringWriter();
        crw.build(sw)
            .writeRecord()
            .endRecord()
            .close();

        assertThat(sw).hasToString("\n");
    }

    @Test
    void simpleRecord() throws IOException {
        final var sw = new StringWriter();
        crw.build(sw)
            .writeRecord().writeField("foo").writeField("bar").endRecord()
            .writeRecord("baz", "qux")
            .close();

        assertThat(sw).hasToString("foo,bar\nbaz,qux\n");
    }

    @Test
    void ioError() {
        final var un = new UnwritableWriter();
        final var csv = crw.bufferSize(0).build(un).writeRecord();

        assertThatThrownBy(() -> csv.writeField("foo"))
            .isInstanceOf(UncheckedIOException.class);

        assertThatThrownBy(csv::endRecord)
            .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void openRecordWriteComment() {
        final CsvWriter csv = crw.build(new StringWriter());
        csv.writeRecord();

        assertThatThrownBy(csv::writeRecord)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Record already started, call end() on CsvWriterRecord first");

        assertThatThrownBy(() -> csv.writeComment("foo"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Record already started, call end() on CsvWriterRecord first");

        assertThatThrownBy(() -> csv.writeRecord("foo"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Record already started, call end() on CsvWriterRecord first");

        assertThatThrownBy(() -> csv.writeRecord(List.of("foo")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Record already started, call end() on CsvWriterRecord first");
    }

}
