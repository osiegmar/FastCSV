package de.siegmar.fastcsv.reader;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RandomAccessFileInputStreamTest {

    private final RandomAccessFileInputStream file = new RandomAccessFileInputStream(null);

    @Test
    void unsupportedRead() {
        Assertions.assertThatThrownBy(file::read)
            .isInstanceOf(UnsupportedOperationException.class);
    }

}
