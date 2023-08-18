package de.siegmar.fastcsv.reader;

/**
 * Custom status listeners have to implement this interface.
 * <p>
 * FastCSV will call these methods synchronously –
 * make sure <strong>not to perform time consuming / blocking</strong> tasks!
 *
 * @see IndexedCsvReader.IndexedCsvReaderBuilder#statusListener(StatusListener)
 */
public interface StatusListener {

    /**
     * Called on initialization.
     *
     * @param totalSize the total file size.
     */
    default void initialize(long totalSize) {
    }

    /**
     * Called when a new row has been read.
     */
    default void readRow() {
    }

    /**
     * Called when a new read operation has been performend.
     *
     * @param bytes number of bytes read.
     */
    default void readBytes(int bytes) {
    }

    /**
     * Called when there was an error while indexing.
     *
     * @param throwable the exception thrown.
     */
    default void failed(Throwable throwable) {
    }

    /**
     * Called when the indexing finished successfully (without an exception).
     */
    default void completed() {
    }

}
