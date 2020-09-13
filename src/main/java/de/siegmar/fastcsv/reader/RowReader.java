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

import java.io.IOException;
import java.io.Reader;

/*
 * This class contains ugly, performance optimized code - be warned!
 */
final class RowReader {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private static final int STATUS_QUOTED_MODE = 4;
    private static final int STATUS_QUOTED_COLUMN = 2;
    private static final int STATUS_DATA_COLUMN = 1;
    private static final int STATUS_RESET = 0;

    private final Buffer buffer;
    private final char fieldSeparator;
    private final char quoteCharacter;

    private char lastChar;
    private int status;

    RowReader(final Reader reader, final char fieldSeparator, final char quoteCharacter) {
        buffer = new Buffer(reader);
        this.fieldSeparator = fieldSeparator;
        this.quoteCharacter = quoteCharacter;
    }

    /**
     * Reads and parses data (one logical row that might span multiple lines) from reader into
     * {@link RowHandler}.
     *
     * @return {@code true} if end of stream reached
     */
    boolean fetchAndRead(final RowHandler rowHandler) throws IOException {
        do {
            if (buffer.len == buffer.pos) {
                // cursor reached current EOD -- need to fetch
                if (buffer.fetchData()) {
                    // reached end of stream
                    if (buffer.begin < buffer.pos) {
                        publishColumn(rowHandler, buffer.buf, buffer.begin,
                            buffer.pos - buffer.begin, status, quoteCharacter);
                    }
                    return true;
                }
            }
        } while (consume(rowHandler));

        return false;
    }

    /**
     * @return {@code true}, if more data is needed to complete current row
     */
    boolean consume(final RowHandler rowHandler) {
        final char[] lBuf = buffer.buf;
        final int lLen = buffer.len;

        int lPos = buffer.pos;
        int lBegin = buffer.begin;
        int lStatus = status;
        char lLastChar = lastChar;

        try {
            do {
                final char c = lBuf[lPos++];

                if ((lStatus & STATUS_QUOTED_MODE) != 0) {
                    // we're in quotes
                    if (c == quoteCharacter) {
                        lStatus &= ~STATUS_QUOTED_MODE;
                    } else if (c == CR || c == LF && lLastChar != CR) {
                        rowHandler.incLines();
                    }
                } else {
                    // we're not in quotes
                    if (c == fieldSeparator) {
                        publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1, lStatus,
                            quoteCharacter);
                        lStatus = STATUS_RESET;
                        lBegin = lPos;
                    } else if (c == LF) {
                        if (lLastChar != CR) {
                            publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1,
                                lStatus, quoteCharacter);
                            lStatus = STATUS_RESET;
                            lBegin = lPos;
                            lLastChar = c;
                            return false;
                        }

                        lBegin = lPos;
                    } else if (c == CR) {
                        publishColumn(rowHandler, lBuf, lBegin, lPos - lBegin - 1, lStatus,
                            quoteCharacter);
                        lStatus = STATUS_RESET;
                        lBegin = lPos;
                        lLastChar = c;
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
        } finally {
            buffer.pos = lPos;
            buffer.begin = lBegin;
            status = lStatus;
            lastChar = lLastChar;
        }

        return true;
    }

    private static void publishColumn(final RowHandler rowHandler, final char[] lBuf,
                                      final int lBegin, final int lPos, final int lStatus,
                                      final char quoteCharacter) {
        if ((lStatus & STATUS_QUOTED_COLUMN) == 0) {
            // column without quotes
            rowHandler.add(lBuf, lBegin, lPos);
        } else {
            // column with quotes
            final int shift = cleanDelimiters(lBuf, lBegin + 1, lBegin + lPos,
                quoteCharacter);
            rowHandler.add(lBuf, lBegin + 1, lPos - 1 - shift);
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

    @SuppressWarnings("checkstyle:visibilitymodifier")
    private static class Buffer {
        private static final int READ_SIZE = 8192;
        private static final int BUFFER_SIZE = READ_SIZE;
        private static final int MAX_BUFFER_SIZE = 8 * 1024 * 1024;

        char[] buf = new char[BUFFER_SIZE];
        int len;
        int begin;
        int pos;

        private final Reader reader;

        Buffer(final Reader reader) {
            this.reader = reader;
        }

        /**
         * @return {@code true}, if EOD reached.
         */
        private boolean fetchData() throws IOException {
            if (begin < pos) {
                // we have data that can be relocated

                if (READ_SIZE > buf.length - pos) {
                    // need to relocate data in buffer -- not enough capacity left

                    final int lenToCopy = pos - begin;
                    if (READ_SIZE > buf.length - lenToCopy) {
                        // need to relocate data in new, larger buffer
                        buf = extendAndRelocate(buf, begin);
                    } else {
                        // relocate data in existing buffer
                        System.arraycopy(buf, begin, buf, 0, lenToCopy);
                    }
                    pos -= begin;
                    begin = 0;
                }
            } else {
                // all data was consumed -- nothing to relocate
                pos = begin = 0;
            }

            final int cnt = reader.read(buf, pos, READ_SIZE);
            if (cnt == -1) {
                return true;
            }
            len = pos + cnt;
            return false;
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

    }

}
