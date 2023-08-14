package de.siegmar.fastcsv.reader;

public interface StatusConsumer {

    void addRecordPosition(int position);

    void addReadBytes(int readCnt);

}
