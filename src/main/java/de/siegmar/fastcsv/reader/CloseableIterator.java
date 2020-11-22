package de.siegmar.fastcsv.reader;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Iterator that supports closing underlying resources.
 *
 * @param <E> the type of elements returned by this iterator
 */
public interface CloseableIterator<E> extends Iterator<E>, Closeable {
}
