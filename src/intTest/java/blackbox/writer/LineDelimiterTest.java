package blackbox.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.writer.LineDelimiter;

class LineDelimiterTest {

    @Test
    void linefeeds() {
        assertThat(LineDelimiter.of("\r\n"))
            .isEqualTo(LineDelimiter.CRLF)
            .asString().isEqualTo("\r\n");

        assertThat(LineDelimiter.of("\n"))
            .isEqualTo(LineDelimiter.LF)
            .asString().isEqualTo("\n");

        assertThat(LineDelimiter.of("\r"))
            .isEqualTo(LineDelimiter.CR)
            .asString().isEqualTo("\r");

        assertThat(LineDelimiter.PLATFORM)
            .asString().isEqualTo(System.lineSeparator());
    }

    @Test
    void illegal() {
        assertThatThrownBy(() -> LineDelimiter.of(";"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown line delimiter: ;");
    }

}
