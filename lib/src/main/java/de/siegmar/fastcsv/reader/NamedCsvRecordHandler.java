package de.siegmar.fastcsv.reader;

@SuppressWarnings("PMD.ArrayIsStoredDirectly")
final class NamedCsvRecordHandler extends AbstractCsvCallbackHandler<NamedCsvRecord> {

    private static final String[] EMPTY_HEADER = new String[0];
    private String[] header;

    NamedCsvRecordHandler() {
    }

    @SuppressWarnings("PMD.UseVarargs")
    NamedCsvRecordHandler(final String[] header) {
        this.header = header.clone();
    }

    @Override
    protected NamedCsvRecord buildRecord(final String[] fields) {
        if (comment) {
            return new NamedCsvRecord(startingLineNumber, fields, true, EMPTY_HEADER);
        }

        if (header == null) {
            header = fields;
            return null;
        }

        return new NamedCsvRecord(startingLineNumber, fields, false, header);
    }

}
