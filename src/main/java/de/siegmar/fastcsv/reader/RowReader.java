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
import java.util.Arrays;

final class RowReader implements Closeable {

    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final int BUFFER_SIZE = 8192;

    private static final int FIELD_MODE_RESET = 0;
    private static final int FIELD_MODE_QUOTED = 1;
    private static final int FIELD_MODE_NON_QUOTED = 2;
    private static final int FIELD_MODE_QUOTE_ON = 4;
    private static final int FIELD_MODE_QUOTED_EMPTY = 8;

    private final Reader reader;
    private final char fieldSeparator;
    private final char textDelimiter;
    private final char[] buf = new char[BUFFER_SIZE];
    private final Line line = new Line(32);
    private final ReusableStringBuilder currentField = new ReusableStringBuilder(512);
    private int bufPos;
    private int bufLen;
    private int prevChar = -1;
    private int copyStart;
    private boolean finished;

    RowReader(final Reader reader, final char fieldSeparator, final char textDelimiter) {
        this.reader = reader;
        this.fieldSeparator = fieldSeparator;
        this.textDelimiter = textDelimiter;
    }

    /*
     * ugly, performance optimized code begins
     */
    Line readLine() throws IOException {
        // get fields local for higher performance
        final Line localLine = line.reset();
        final ReusableStringBuilder localCurrentField = currentField;
        final char[] localBuf = buf;
        int localBufPos = bufPos;
        int localPrevChar = prevChar;
        int localCopyStart = copyStart;

        int copyLen = 0;
        int fieldMode = FIELD_MODE_RESET;
        int lines = 1;

        while (true) {
            if (bufLen == localBufPos) {
                // end of buffer

                if (copyLen > 0) {
                    localCurrentField.append(localBuf, localCopyStart, copyLen);
                }
                bufLen = reader.read(localBuf, 0, localBuf.length);

                if (bufLen < 0) {
                    // end of data
                    finished = true;

                    if (localPrevChar == fieldSeparator
                            || (fieldMode & FIELD_MODE_QUOTED_EMPTY) == FIELD_MODE_QUOTED_EMPTY
                            || localCurrentField.hasContent()
                    ) {
                        localLine.addField(localCurrentField.toStringAndReset());
                    }

                    break;
                }

                localCopyStart = localBufPos = copyLen = 0;
            }

            final char c = localBuf[localBufPos++];

            if ((fieldMode & FIELD_MODE_QUOTE_ON) != 0) {
                if (c == textDelimiter) {
                    // End of quoted text
                    fieldMode &= ~FIELD_MODE_QUOTE_ON;
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                        copyLen = 0;
                    } else {
                        fieldMode |= FIELD_MODE_QUOTED_EMPTY;
                    }
                    localCopyStart = localBufPos;
                } else {
                    if (c == CR || c == LF && prevChar != CR) {
                        lines++;
                    }
                    copyLen++;
                }
            } else {
                if (c == fieldSeparator) {
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                        copyLen = 0;
                    }
                    localLine.addField(localCurrentField.toStringAndReset());
                    localCopyStart = localBufPos;
                    fieldMode = FIELD_MODE_RESET;
                } else if (c == textDelimiter && (fieldMode & FIELD_MODE_NON_QUOTED) == 0) {
                    // Quoted text starts
                    fieldMode = FIELD_MODE_QUOTED | FIELD_MODE_QUOTE_ON;

                    if (localPrevChar == textDelimiter) {
                        // escaped quote
                        copyLen++;
                    } else {
                        localCopyStart = localBufPos;
                    }
                } else if (c == CR) {
                    if (copyLen > 0) {
                        localCurrentField.append(localBuf, localCopyStart, copyLen);
                    }
                    localLine.addField(localCurrentField.toStringAndReset());
                    localPrevChar = c;
                    localCopyStart = localBufPos;
                    break;
                } else if (c == LF) {
                    if (localPrevChar != CR) {
                        if (copyLen > 0) {
                            localCurrentField.append(localBuf, localCopyStart, copyLen);
                        }
                        localLine.addField(localCurrentField.toStringAndReset());
                        localPrevChar = c;
                        localCopyStart = localBufPos;
                        break;
                    }
                    localCopyStart = localBufPos;
                } else {
                    copyLen++;
                    if (fieldMode == FIELD_MODE_RESET) {
                        fieldMode = FIELD_MODE_NON_QUOTED;
                    }
                }
            }

            localPrevChar = c;
        }

        // restore fields
        bufPos = localBufPos;
        prevChar = localPrevChar;
        copyStart = localCopyStart;

        localLine.setLines(lines);
        return localLine;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public boolean isFinished() {
        return finished;
    }

    static final class Line {

        private String[] fields;
        private int linePos;
        private int lines;

        Line(final int initialCapacity) {
            fields = new String[initialCapacity];
        }

        Line reset() {
            linePos = 0;
            lines = 1;
            return this;
        }

        void addField(final String field) {
            if (linePos == fields.length) {
                fields = Arrays.copyOf(fields, fields.length * 2);
            }
            fields[linePos++] = field;
        }

        String[] getFields() {
            return Arrays.copyOf(fields, linePos);
        }

        int getLines() {
            return lines;
        }

        void setLines(final int lines) {
            this.lines = lines;
        }

    }

}
