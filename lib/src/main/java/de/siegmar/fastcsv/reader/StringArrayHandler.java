package de.siegmar.fastcsv.reader;

final class StringArrayHandler extends AbstractCsvCallbackHandler<String[]> {

    @Override
    protected String[] buildRecord(final String[] fields) {
        return fields;
    }

}
