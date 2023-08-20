package de.siegmar.fastcsv.reader;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link StatusListener} that collects updates.
 */
public class CollectingStatusListener implements StatusListener {

    private volatile long totalSize;
    private final AtomicLong rowCount = new AtomicLong();
    private final AtomicLong byteCount = new AtomicLong();
    private volatile boolean completionStatus;
    private volatile Throwable failedThrowable;

    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void onInit(final long totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * Get the total size in bytes.
     *
     * @return the total size in bytes
     */
    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public void onReadRow() {
        rowCount.incrementAndGet();
    }

    /**
     * Get the number of rows already indexed.
     *
     * @return the number of rows already indexed
     */
    public long getRowCount() {
        return rowCount.longValue();
    }

    @Override
    public void onReadBytes(final int bytes) {
        byteCount.addAndGet(bytes);
    }

    /**
     * Get the number of bytes already read.
     *
     * @return the number of bytes already read
     */
    public long getByteCount() {
        return byteCount.longValue();
    }

    @Override
    public void onComplete() {
        completionStatus = true;
    }

    /**
     * Get the completion status.
     *
     * @return {@code true}, when all data have been indexed successfully
     */
    public boolean isCompleted() {
        return completionStatus;
    }

    @Override
    public void onError(final Throwable throwable) {
        this.failedThrowable = throwable;
    }

    /**
     * Get the throwable that occurred while indexing.
     *
     * @return the throwable that occurred while indexing, {@code null} otherwise.
     */
    public Throwable getThrowable() {
        return failedThrowable;
    }

    @Override
    public String toString() {
        final long byteCntVal = this.byteCount.longValue();
        final double percentage = byteCntVal * 100.0 / totalSize;
        return String.format("Read %,d rows and %,d of %,d bytes (%.2f %%)",
            rowCount.longValue(), byteCntVal, totalSize, percentage);
    }

}
