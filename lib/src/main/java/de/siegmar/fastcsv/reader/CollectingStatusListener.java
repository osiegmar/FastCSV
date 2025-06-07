package de.siegmar.fastcsv.reader;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// Implementation of [StatusListener] that collects updates.
public class CollectingStatusListener implements StatusListener {

    private final AtomicLong fileSize = new AtomicLong();
    private final AtomicLong recordCount = new AtomicLong();
    private final AtomicLong byteCount = new AtomicLong();
    private final AtomicBoolean completionStatus = new AtomicBoolean();
    private final AtomicReference<Throwable> failedThrowable = new AtomicReference<>();

    /// Default constructor.
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public CollectingStatusListener() {
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void onInit(final long fileSize) {
        this.fileSize.set(fileSize);
    }

    /// Get the total size in bytes.
    ///
    /// @return the total size in bytes
    public long getFileSize() {
        return fileSize.get();
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
        completionStatus.set(true);
    }

    /// Get the completion status.
    ///
    /// @return `true`, when all data have been indexed successfully
    public boolean isCompleted() {
        return completionStatus.get();
    }

    @Override
    public void onError(final Throwable throwable) {
        failedThrowable.set(throwable);
    }

    /// Get the throwable that occurred while indexing.
    ///
    /// @return the throwable that occurred while indexing, `null` otherwise.
    public Throwable getThrowable() {
        return failedThrowable.get();
    }

    @Override
    public String toString() {
        final long byteCntVal = byteCount.longValue();
        final long currentFileSize = fileSize.get();
        final double percentage = byteCntVal * 100.0 / currentFileSize;
        return "Read %,d records and %,d of %,d bytes (%.2f %%)"
            .formatted(recordCount.longValue(), byteCntVal, currentFileSize, percentage);
    }

}
