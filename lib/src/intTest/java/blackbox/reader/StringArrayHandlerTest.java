package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.reader.StringArrayHandler;

class StringArrayHandlerTest {

    private static final String TEST_DATA = " foo , bar ";

    @Test
    void defaultHandler() {
        final StringArrayHandler handler = StringArrayHandler.of();
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .containsExactly(new String[]{" foo ", " bar "});
    }

    @Test
    void builder() {
        final StringArrayHandler handler = StringArrayHandler.builder()
            .fieldModifier(FieldModifiers.TRIM)
            .build();
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .containsExactly(new String[]{"foo", "bar"});
    }

    @Test
    void consumer() {
        final StringArrayHandler handler = StringArrayHandler.of(c -> c
            .fieldModifier(FieldModifiers.TRIM)
        );
        assertThat(CsvReader.builder().build(handler, TEST_DATA).stream())
            .containsExactly(new String[]{"foo", "bar"});
    }

}
