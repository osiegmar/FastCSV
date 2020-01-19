package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class CsvWriterExampleTest {

    @Test
    public void test() {
        final Collection<String[]> rows = Collections.singleton(
            new String[]{"foo", "bar"}
        );

        final Writer sw = new StringWriter();

        CsvWriter.builder()
            .fieldSeparator(',')
            .textDelimiter('"')
            .textDelimitStrategy(TextDelimitStrategy.REQUIRED)
            .lineDelimiter("\n")
            .writeAll(rows, sw);

        assertEquals("foo,bar\n", sw.toString());
    }

}
