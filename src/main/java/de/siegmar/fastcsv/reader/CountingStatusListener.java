package de.siegmar.fastcsv.reader;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple implementation of {@link StatusListener} to count read rows and bytes.
 */
public class CountingStatusListener implements StatusListener {

    protected volatile long totalSize;
    protected final AtomicLong rowCnt = new AtomicLong();
    protected final AtomicLong byteCnt = new AtomicLong();

    @SuppressWarnings("checkstyle:HiddenField")
    @Override
    public void initialize(final long totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public void readRow() {
        rowCnt.incrementAndGet();
    }

    @Override
    public void readBytes(final int bytes) {
        byteCnt.addAndGet(bytes);
    }

    @Override
    public String toString() {
        final long byteCntVal = this.byteCnt.longValue();
        final double percentage = byteCntVal * 100.0 / totalSize;
        return String.format("Read %,d rows and %,d of %,d bytes (%.2f %%)",
            rowCnt.longValue(), byteCntVal, totalSize, percentage);
    }

}
