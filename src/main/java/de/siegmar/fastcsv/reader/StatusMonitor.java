package de.siegmar.fastcsv.reader;

/**
 *
 */
public interface StatusMonitor {

    /**
     * Gets the current number of records found in CSV file.
     *
     * @return the current number of records found in CSV file.
     */
    long getRecordCount();

    /**
     * Gets the current number of bytes read from the CSV file.
     *
     * @return the current number of bytes read from the CSV file.
     */
    long getReadBytes();

}
