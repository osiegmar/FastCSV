package de.siegmar.fastcsv.reader;

interface StatusConsumer {

    default void addRowPosition(int position) {
    }

    default void addReadBytes(int readCnt) {
    }

}
