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

    private char[] buf;
    private boolean distinguishNullAndEmpty;
    private int pos;
    private boolean hasContent;

    /**
     * Initializes the buffer with the specified capacity.
     *
     * @param initialCapacity the initial buffer capacity.
     */
    ReusableStringBuilder(final int initialCapacity) {
        this(initialCapacity, false);
    }

    /**
     * Initializes the buffer with the specified capacity.
     *
     * @param initialCapacity the initial buffer capacity.
     * @param distinguishNullAndEmpty whether to distinguish null and empty column values.
     */
    ReusableStringBuilder(final int initialCapacity, final boolean distinguishNullAndEmpty) {
        this.buf = new char[initialCapacity];
        this.distinguishNullAndEmpty = distinguishNullAndEmpty;
    }

    /**
     * Marks the object has having data, even if nothing has been appended.
     */
    public void markHasContent() {
        hasContent = true;
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
        return (pos > 0) || hasContent;
    }

    /**
     * Returns the string representation of the buffer and resets the buffer.
     *
     * @return the string representation of the buffer
     */
    public String toStringAndReset() {
        String result = null;

        if (pos > 0) {
            result = new String(buf, 0, pos);
        } else if (hasContent()) {
            result = "";
        } else {
            if (distinguishNullAndEmpty) {
                result = null;
            } else {
                result = "";
            }
        }

        pos = 0;
        hasContent = false;
        return result;
    }
}
