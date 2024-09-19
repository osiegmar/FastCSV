package de.siegmar.fastcsv.writer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * This interface extends the basic functionality provided by {@link java.io.Writer}
 * with the addition of the {@link #endRecord()} method.
 */
interface Writable extends Closeable, Flushable {

    /**
     * Writes a single character.
     * @param c the character to write
     * @see java.io.Writer#write(int)
     */
    void write(int c) throws IOException;

    /**
     * Writes a portion of a string.
     * @param value the string to write
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @see java.io.Writer#write(String, int, int)
     */
    void write(String value, int off, int len) throws IOException;

    /**
     * Writes a portion of an array of characters.
     * @param value the array of characters to write
     * @param off the offset from which to start writing characters
     * @param len the number of characters to write
     * @see java.io.Writer#write(char[], int, int)
     */
    void write(char[] value, int off, int len) throws IOException;

    /**
     * Called to indicate that the current record is complete.
     * @throws IOException if an I/O error occurs
     */
    void endRecord() throws IOException;

}
