package de.siegmar.fastcsv.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CachingWriterTest {

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void appendSingle(final Appendable sw, final CsvWriter.CachingWriter cw) throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write('a');
            cw.write('b');
        }
        cw.close();

        assertEquals(sb.toString(), sw.toString());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void appendArray(final Appendable sw, final CsvWriter.CachingWriter cw) throws IOException {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
            cw.write("ab", 0, 2);
        }
        cw.close();

        assertEquals(sb.toString(), sw.toString());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void appendLarge(final Appendable sw, final CsvWriter.CachingWriter cw) throws IOException {
        final String sb = buildLargeData();
        cw.write(sb, 0, sb.length());

        assertEquals(sb, sw.toString());
    }

    private String buildLargeData() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8192; i++) {
            sb.append("ab");
        }
        return sb.toString();
    }

    static Stream<?> provideArguments() {
        final StringWriter stringWriter = new StringWriter();
        final CsvWriter.CachingWriter cachingWriterWithStringWriter = new  CsvWriter.CachingWriter(stringWriter);

        final StringBuilder stringBuilder = new StringBuilder();
        final CsvWriter.CachingWriter cachingWriterWithStringBuilder = new  CsvWriter.CachingWriter(stringBuilder);
        return Stream.of(
                Arguments.of(stringWriter, cachingWriterWithStringWriter),
                Arguments.of(stringBuilder, cachingWriterWithStringBuilder));
    }

}
