package de.siegmar.fastcsv.reader;

public interface StatusConsumer {

    void addPosition(int position);
    void addReadBytes(int readCnt);

}
