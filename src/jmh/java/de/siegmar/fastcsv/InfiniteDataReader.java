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

package de.siegmar.fastcsv;

import java.io.Reader;

class InfiniteDataReader extends Reader {

    private final char[] data;
    private int pos;

    InfiniteDataReader(final String data) {
        this.data = data.toCharArray();
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) {
        int copied = 0;
        while (copied < len) {
            final int tlen = Math.min(len - copied, data.length - pos);
            System.arraycopy(data, pos, cbuf, off + copied, tlen);
            copied += tlen;
            pos += tlen;

            if (pos == data.length) {
                pos = 0;
            }
        }

        return copied;
    }

    @Override
    public void close() {
        // NOP
    }

}
