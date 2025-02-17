package de.siegmar.fastcsv.reader;

import java.util.concurrent.atomic.AtomicLong;

/// Implementation of [StatusListener] that collects updates.
public class CollectingStatusListener implements StatusListener {

    private volatile long fileSize;
    private final AtomicLong recordCount = new AtomicLong();
    private final AtomicLong byteCount = new AtomicLong();
    private volatile boolean completionStatus;
    private volatile Throwable failedThrowable;

    /// Default constructor.
    public CollectingStatusListener() {
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void onInit(final long fileSize) {
        this.fileSize = fileSize;
    }

    /// Get the total size in bytes.
    ///
    /// @return the total size in bytes
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void onReadRecord() {
        recordCount.incrementAndGet();
    }

    /// Get the number of records already indexed.
    ///
    /// @return the number of records already indexed
    public long getRecordCount() {
        return recordCount.longValue();
    }

    @Override
    public void onReadBytes(final int bytes) {
        byteCount.addAndGet(bytes);
    }

    /// Get the number of bytes already read.
    ///
    /// @return the number of bytes already read
    public long getByteCount() {
        return byteCount.longValue();
    }

    @Override
    public void onComplete() {
        completionStatus = true;
    }

    /// Get the completion status.
    ///
    /// @return `true`, when all data have been indexed successfully
    public boolean isCompleted() {
        return completionStatus;
    }

    @Override
    public void onError(final Throwable throwable) {
        this.failedThrowable = throwable;
    }

    /// Get the throwable that occurred while indexing.
    ///
    /// @return the throwable that occurred while indexing, `null` otherwise.
    public Throwable getThrowable() {
        return failedThrowable;
    }

    @Override
    public String toString() {
        final long byteCntVal = byteCount.longValue();
        final double percentage = byteCntVal * 100.0 / fileSize;
        return String.format("Read %,d records and %,d of %,d bytes (%.2f %%)",
            recordCount.longValue(), byteCntVal, fileSize, percentage);
    }

}
