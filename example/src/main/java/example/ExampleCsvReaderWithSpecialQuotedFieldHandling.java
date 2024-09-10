package example;

import java.util.ArrayList;
import java.util.List;

import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.RecordWrapper;

/**
 * Example for reading CSV data with custom handling of quoted fields.
 */
public class ExampleCsvReaderWithSpecialQuotedFieldHandling {

    private static final String DATA = """
        "quoted foo",unquoted foo
        """;

    public static void main(final String[] args) {
        CsvReader.builder()
            .skipEmptyLines(false)
            .build(new QuotableFieldHandler(), DATA)
            .forEach(System.out::println);
    }

    static class QuotableFieldHandler extends AbstractBaseCsvCallbackHandler<List<QuotableField>> {

        private final List<QuotableField> fields = new ArrayList<>();

        @Override
        protected void handleField(final int fieldIdx,
                                   final char[] buf, final int offset, final int len,
                                   final boolean quoted) {
            final String value = new String(buf, offset, len);
            fields.add(new QuotableField(value, quoted));
        }

        @Override
        protected void handleBegin(final long startingLineNumber) {
            fields.clear();
        }

        @Override
        protected RecordWrapper<List<QuotableField>> buildRecord() {
            return wrapRecord(List.copyOf(fields));
        }

    }

    record QuotableField(String field, boolean quoted) {
    }

}
