package de.siegmar.fastcsv.reader;

public interface StatusConsumer {

    default void addRecordPosition(int position) {
    }

    default void addReadBytes(int readCnt) {
    }

}
