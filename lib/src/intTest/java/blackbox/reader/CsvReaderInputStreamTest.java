package blackbox.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

class CsvReaderInputStreamTest {

    @Test
    void csvRecordOfInputStream() {
        final Stream<CsvRecord> csvStream = CsvReader.builder().ofCsvRecord(testData()).stream();
        assertThat(csvStream)
            .satisfiesExactly(
                r -> assertThat(r.getFields()).containsExactly("foo"),
                r -> assertThat(r.getFields()).containsExactly("bar")
            );
    }

    @Test
    void csvRecordOfInputStreamWEncoding() {
        final Stream<CsvRecord> csvStream = CsvReader.builder().ofCsvRecord(testData(), UTF_8).stream();
        assertThat(csvStream)
            .satisfiesExactly(
                r -> assertThat(r.getFields()).containsExactly("foo"),
                r -> assertThat(r.getFields()).containsExactly("bar")
            );
    }

    @Test
    void namedCsvRecordOfInputStream() {
        final Stream<NamedCsvRecord> csvStream = CsvReader.builder().ofNamedCsvRecord(testData()).stream();
        assertThat(csvStream)
            .satisfiesExactly(
                r -> assertThat(r.getFieldsAsMap()).containsExactly(Map.entry("foo", "bar"))
            );
    }

    @Test
    void namedCsvRecordOfInputStreamWEncoding() {
        final Stream<NamedCsvRecord> csvStream = CsvReader.builder().ofNamedCsvRecord(testData(), UTF_8).stream();
        assertThat(csvStream)
            .satisfiesExactly(
                r -> assertThat(r.getFieldsAsMap()).containsExactly(Map.entry("foo", "bar"))
            );
    }

    private static InputStream testData() {
        return new ByteArrayInputStream("foo\nbar".getBytes(UTF_8));
    }

}
