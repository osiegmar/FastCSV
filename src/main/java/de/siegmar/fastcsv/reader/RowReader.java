/*
 * Copyright 2018 Oliver Siegmar
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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

final class RowReader implements Closeable {

    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final int READ_SIZE = 8192;
    private static final int BUFFER_SIZE = READ_SIZE;
    private static final int MAX_BUFFER_SIZE = 8 * 1024 * 1024;

    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_COLUMN = 2;
    private static final int STATUS_DATA_COLUMN = 1;
    private static final int STATUS_RESET = 0;

    private final Reader reader;
    private final char fieldSeparator;
    private final char quoteCharacter;

    private char[] buf = new char[BUFFER_SIZE];
    private int len;
    private int begin;
    private int pos;
    private char lastChar;

    RowReader(final Reader reader, final char fieldSeparator, final char quoteCharacter) {
        this.reader = reader;
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
    }

    /**
     * Reads and parses data (one logical row that might span multiple lines) from reader into
     * {@link RowHandler}.
     *
     * @return {@code true} if end of stream reached
     */
    // ugly, performance optimized code begins
    boolean readRow(final RowHandler rowHandler) throws IOException {
        int lines = 1;

        int lPos = pos;
        int lBegin = begin;
        char[] lBuf = buf;
        char lLastChar = lastChar;
        int lStatus = STATUS_RESET;
        int lLen = len;

        while (true) {
            if (lLen == lPos) {
                // cursor reached current EOD -- need to fetch

                if (lBegin < lPos) {
                    // we have data that can be relocated

                    if (READ_SIZE > lBuf.length - lPos) {
                        // need to relocate data in buffer -- not enough capacity left

                        final int lenToCopy = lPos - lBegin;
                        if (READ_SIZE > lBuf.length - lenToCopy) {
                            // need to relocate data in new, larger buffer
                            buf = lBuf = extendAndRelocate(lBuf, lBegin);
                        } else {
                            // relocate data in existing buffer
                            System.arraycopy(lBuf, lBegin, lBuf, 0, lenToCopy);
                        }
                        lPos -= lBegin;
                        lBegin = 0;
                    }
                } else {
                    // all data was consumed -- nothing to relocate
                    lPos = lBegin = 0;
                }

                final int cnt = reader.read(lBuf, lPos, READ_SIZE);
                if (cnt == -1) {
                    // reached end of stream
                    if (lBegin < lPos) {
                        publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin, lStatus, lines);
                    }
                    return true;
                }
                len = lLen = lPos + cnt;
            }

            do {
                final char c = lBuf[lPos++];

                if ((lStatus & STATUS_QUOTED_MODE) != 0) {
                    // we're in quotes
                    if (c == quoteCharacter) {
                        lStatus &= ~STATUS_QUOTED_MODE;
                    } else if (c == CR || c == LF && lLastChar != CR) {
                        lines++;
                    }
                } else {
                    // we're not in quotes
                    if (c == fieldSeparator) {
                        publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1, lStatus, lines);
                        lStatus = STATUS_RESET;
                        lBegin = lPos;
                    } else if (c == LF) {
                        if (lLastChar != CR) {
                            publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1,
                                lStatus, lines);
                            pos = begin = lPos;
                            lastChar = c;
                            return false;
                        }

                        lBegin = lPos;
                    } else if (c == CR) {
                        publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1, lStatus, lines);
                        pos = begin = lPos;
                        lastChar = c;
                        return false;
                    } else if (c == quoteCharacter && (lStatus & STATUS_DATA_COLUMN) == 0) {
                        // quote and not in data-only mode
                        lStatus |= STATUS_QUOTED_COLUMN | STATUS_QUOTED_MODE;
                    } else if ((lStatus & STATUS_QUOTED_COLUMN) == 0) {
                        lStatus |= STATUS_DATA_COLUMN;
                    }
                }

                lLastChar = c;
            } while (lPos < lLen);
        }
    }

    private static char[] extendAndRelocate(final char[] buf, final int begin) {
        final int newBufferSize = buf.length * 2;
        if (newBufferSize > MAX_BUFFER_SIZE) {
            throw new IllegalStateException("Maximum buffer size "
                + MAX_BUFFER_SIZE + " is not enough to read data");
        }
        final char[] newBuf = new char[newBufferSize];
        System.arraycopy(buf, begin, newBuf, 0, buf.length - begin);
        return newBuf;
    }

    private void publishColumn(final RowHandler rowHandler, final char[] lBuf,
                               final int lBegin, final int lPos, final int status,
                               final int lines) {
        if ((status & STATUS_QUOTED_COLUMN) == 0) {
            // column without quotes
            rowHandler.add(lBuf, lBegin, lPos, lines);
        } else {
            // column with quotes
            final int shift = cleanDelimiters(lBuf, lBegin + 1, lBegin + lPos, quoteCharacter);
            rowHandler.add(lBuf, lBegin + 1, lPos - 1 - shift, lines);
        }
    }

    private static int cleanDelimiters(final char[] buf, final int begin, final int pos,
                                       final char quoteCharacter) {
        int shift = 0;
        boolean escape = false;
        for (int i = begin; i < pos; i++) {
            final char c = buf[i];

            if (c == quoteCharacter) {
                if (!escape) {
                    shift++;
                    escape = true;
                    continue;
                } else {
                    escape = false;
                }
            }

            if (shift > 0) {
                buf[i - shift] = c;
            }
        }

        return shift;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
