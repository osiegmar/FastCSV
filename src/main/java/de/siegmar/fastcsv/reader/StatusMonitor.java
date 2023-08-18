package de.siegmar.fastcsv.reader;

/**
 * @see RandomAccessCsvReader#getStatusMonitor()
 */
public interface StatusMonitor {

    /**
     * Gets the current number of rows found in CSV file.
     *
     * @return the current number of rows found in CSV file.
     */
    long getRowCount();

    /**
     * Gets the current number of bytes read from the CSV file.
     *
     * @return the current number of bytes read from the CSV file.
     */
    long getReadBytes();

}
