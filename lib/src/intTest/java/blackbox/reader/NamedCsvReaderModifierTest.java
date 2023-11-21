package blackbox.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import testutil.NamedCsvRecordAssert;

class NamedCsvReaderModifierTest {

    private final NamedCsvReader.NamedCsvReaderBuilder crb = NamedCsvReader.builder();

    @Test
    void trim() {
        crb.fieldModifier(FieldModifier.TRIM);

        assertThat(crb.build("  foo  \n   bar   ").stream())
            .singleElement(NamedCsvRecordAssert.NAMED_CSV_RECORD)
            .field("foo")
            .isEqualTo("bar");
    }

}
