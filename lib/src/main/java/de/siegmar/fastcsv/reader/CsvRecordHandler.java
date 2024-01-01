package de.siegmar.fastcsv.reader;

final class CsvRecordHandler extends AbstractCsvCallbackHandler<CsvRecord> {

    @Override
    protected CsvRecord buildRecord(final String[] fields) {
        return new CsvRecord(startingLineNumber, fields, comment);
    }

}
