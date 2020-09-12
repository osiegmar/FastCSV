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

package de.siegmar.fastcsv.writer;

import java.io.IOException;
import java.io.Writer;

/**
 * Unsynchronized and thus high performance replacement for BufferedWriter.
 *
 * This class is intended for internal use only.
 */
public final class FastBufferedWriter extends Writer {

    private static final int BUFFER_SIZE = 8192;

    private final Writer out;
    private final char[] buf = new char[BUFFER_SIZE];
    private int pos;

    public FastBufferedWriter(final Writer writer) {
        this.out = writer;
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    @Override
    public void write(final char[] cbuf, int off, int len) throws IOException {
        do {
            final int copyLen = Math.min(BUFFER_SIZE - pos, len);
            System.arraycopy(cbuf, off, buf, pos, copyLen);
            pos += copyLen;
            off += copyLen;
            len -= copyLen;
            if (pos >= BUFFER_SIZE) {
                flushBuffer();
            }
        } while (len > 0);
    }

    @Override
    public void write(final int c) throws IOException {
        buf[pos++] = (char) c;
        if (pos >= BUFFER_SIZE) {
            flushBuffer();
        }
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    @Override
    public void write(final String str, int off, int len) throws IOException {
        do {
            final int copyLen = Math.min(BUFFER_SIZE - pos, len);
            str.getChars(off, off + copyLen, buf, pos);
            pos += copyLen;
            off += copyLen;
            len -= copyLen;
            if (pos >= BUFFER_SIZE) {
                flushBuffer();
            }
        } while (len > 0);
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        out.close();
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }

    private void flushBuffer() throws IOException {
        out.write(buf, 0, pos);
        pos = 0;
    }

}
