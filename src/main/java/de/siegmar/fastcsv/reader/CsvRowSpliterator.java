/*
 * Copyright 2020 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.fastcsv.reader;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class CsvRowSpliterator<T extends CsvRow> implements Spliterator<T> {

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
