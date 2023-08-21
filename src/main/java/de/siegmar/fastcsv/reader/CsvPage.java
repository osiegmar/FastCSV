package de.siegmar.fastcsv.reader;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
final class CsvPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long offset;
    private final long startingLineNumber;

    CsvPage(final long offset, final long startingLineNumber) {
        this.offset = offset;
        this.startingLineNumber = startingLineNumber;
    }

    long offset() {
        return offset;
    }

    long startingLineNumber() {
        return startingLineNumber;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CsvPage csvPage = (CsvPage) o;
        return offset == csvPage.offset
            && startingLineNumber == csvPage.startingLineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, startingLineNumber);
    }

}
