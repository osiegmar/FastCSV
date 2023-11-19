package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.FieldModifier;
import testutil.CsvRecordAssert;

class CsvReaderModifierTest {

    private final CsvReader.CsvReaderBuilder crb = CsvReader.builder();

    @Test
    void trim() {
        crb.fieldModifier(FieldModifier.TRIM);

        assertThat(crb.build("foo, bar\u2000 ,baz").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar\u2000", "baz");
    }

    @Test
    void strip() {
        crb.fieldModifier(FieldModifier.STRIP);

        assertThat(crb.build("foo, bar\u2000 ,baz").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar", "baz");
    }

    @Test
    void combination() {
        final FieldModifier addSpaces = (originalLineNumber, fieldNo, comment, field) ->
            " " + field + " ";

        crb.fieldModifier(addSpaces
            .andThen(FieldModifier.upper(Locale.ROOT))
            .andThen(FieldModifier.lower(Locale.ROOT))
            .andThen(FieldModifier.TRIM)
        );

        assertThat(crb.build("FOO, bar , BAZ  ").stream())
            .singleElement(CsvRecordAssert.CSV_RECORD)
            .fields()
            .containsExactly("foo", "bar", "baz");
    }

}
