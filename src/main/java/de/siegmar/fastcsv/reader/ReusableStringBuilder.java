/*
 * Copyright 2015 Oliver Siegmar
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

import java.util.Arrays;

/**
 * Resettable / reusable and thus high performance replacement for StringBuilder.
 *
 * This class is intended for internal use only.
 *
 * @author Oliver Siegmar
 */

final class ReusableStringBuilder {

    private static final String EMPTY = "";

    private char[] buf;
    private int pos;

    /**
     * Initializes the buffer with the specified capacity.
     *
     * @param initialCapacity the initial buffer capacity.
     */
    ReusableStringBuilder(final int initialCapacity) {
        buf = new char[initialCapacity];
    }

    /**
     * Appends a character to the buffer, resizing the buffer if needed.
     *
     * @param c the character to add to the buffer
     */
    public void append(final char c) {
        if (pos == buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[pos++] = c;
    }

    public void append(final char[] src, final int srcPos, final int length) {
        if (pos + length > buf.length) {
            int newSize = buf.length * 2;
            while (pos + length > newSize) {
                newSize *= 2;
            }
            buf = Arrays.copyOf(buf, newSize);
        }
        System.arraycopy(src, srcPos, buf, pos, length);
        pos += length;
    }

    /**
     * @return {@code true} if the buffer contains content
     */
    public boolean hasContent() {
        return pos > 0;
    }

    /**
     * Returns the string representation of the buffer and resets the buffer.
     *
     * @return the string representation of the buffer
     */
    public String toStringAndReset() {
        if (pos > 0) {
            final String s = new String(buf, 0, pos);
            pos = 0;
            return s;
        }
        return EMPTY;
    }

}
