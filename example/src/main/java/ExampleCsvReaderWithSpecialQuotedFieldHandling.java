import java.util.ArrayList;
import java.util.List;

import de.siegmar.fastcsv.reader.AbstractBaseCsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;

/// Example for reading CSV data with custom handling of quoted fields.
///
/// FastCSV supports Java 17 and later, but this code uses Java 25
/// for brevity, leveraging newer language features.
void main() {
    final String data = """
        "quoted foo",unquoted foo
        """;

    CsvReader.builder()
        .skipEmptyLines(false)
        .build(new QuotableFieldHandler(), data)
        .forEach(IO::println);
}

static class QuotableFieldHandler
    extends AbstractBaseCsvCallbackHandler<List<QuotableField>> {

    private final List<QuotableField> fields = new ArrayList<>();

    @Override
    protected void handleField(final int fieldIdx,
                               final char[] buf,
                               final int offset, final int len,
                               final boolean quoted) {
        final String value = new String(buf, offset, len);
        fields.add(new QuotableField(value, quoted));
    }

    @Override
    protected void handleBegin(final long startingLineNumber) {
        fields.clear();
    }

    @Override
    protected List<QuotableField> buildRecord() {
        return List.copyOf(fields);
    }

}

record QuotableField(String field, boolean quoted) {
}
