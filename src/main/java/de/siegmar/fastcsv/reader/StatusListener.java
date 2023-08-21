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
     * @param fileSize the total file size.
     */
    default void onInit(long fileSize) {
    }

    /**
     * Called when a new row has been read.
     */
    default void onReadRow() {
    }

    /**
     * Called when a new read operation has been performend.
     *
     * @param bytes number of bytes read.
     */
    default void onReadBytes(int bytes) {
    }

    /**
     * Called when the indexing finished successfully (without an exception).
     */
    default void onComplete() {
    }

    /**
     * Called when there was an error while indexing.
     *
     * @param throwable the exception thrown.
     */
    default void onError(Throwable throwable) {
    }

}
