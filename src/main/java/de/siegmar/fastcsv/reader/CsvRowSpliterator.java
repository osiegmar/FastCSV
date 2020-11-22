package de.siegmar.fastcsv.reader;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

final class CsvRowSpliterator<T extends CsvRow> implements Spliterator<T> {

    private static final int CHARACTERISTICS = ORDERED | DISTINCT | NONNULL | IMMUTABLE;

    private final Iterator<T> iterator;

    CsvRowSpliterator(final Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        if (!iterator.hasNext()) {
            return false;
        }

        action.accept(iterator.next());
        return true;
    }

    @Override
    public void forEachRemaining(final Consumer<? super T> action) {
        iterator.forEachRemaining(action);
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return CHARACTERISTICS;
    }

}
