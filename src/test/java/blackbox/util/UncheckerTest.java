package blackbox.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.siegmar.fastcsv.util.Unchecker;

public class UncheckerTest {

    @Test
    public void uncheck() {
        assertThrows(IOException.class, () -> Unchecker.uncheck(new IOException()));
    }

}
