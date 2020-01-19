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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Objects;

/**
 * This is the main class for writing CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvWriter implements Closeable, Flushable {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private final Writer writer;
    private final char fieldSeparator;
    private final char textDelimiter;
    private final TextDelimitStrategy textDelimitStrategy;
    private final String lineDelimiter;

    private boolean newline = true;

    public static CsvWriterBuilder builder() {
        return new CsvWriterBuilder();
    }

    CsvWriter(final Writer writer, final char fieldSeparator, final char textDelimiter,
              final TextDelimitStrategy textDelimitStrategy, final char[] lineDelimiter) {
        this.writer = writer;
        this.fieldSeparator = fieldSeparator;
        this.textDelimiter = textDelimiter;
        this.textDelimitStrategy = Objects.requireNonNull(textDelimitStrategy);
        this.lineDelimiter = new String(lineDelimiter);
    }

    /**
     * Appends a field to the current row. Automatically adds field separator and text delimiters
     * as required.
     *
     * @param value the field to append (can be {@code null})
     * @throws UncheckedIOException if a write error occurs
     */
    public void appendField(final String value) {
        if (!newline) {
            write(fieldSeparator);
        } else {
            newline = false;
        }

        if (value == null) {
            if (textDelimitStrategy == TextDelimitStrategy.ALWAYS) {
                write(textDelimiter);
                write(textDelimiter);
            }
            return;
        }

        if (value.isEmpty()) {
            if (textDelimitStrategy == TextDelimitStrategy.ALWAYS
                || textDelimitStrategy == TextDelimitStrategy.EMPTY) {
                write(textDelimiter);
                write(textDelimiter);
            }
            return;
        }

        final int length = value.length();
        boolean needsTextDelimiter = textDelimitStrategy == TextDelimitStrategy.ALWAYS;
        int nextDelimPos = -1;

        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c == textDelimiter) {
                needsTextDelimiter = true;
                nextDelimPos = i;
                break;
            }
            if (!needsTextDelimiter && (c == fieldSeparator || c == LF || c == CR)) {
                needsTextDelimiter = true;
            }
        }

        if (needsTextDelimiter) {
            write(textDelimiter);
        }

        if (nextDelimPos > -1) {
            appendEscaped(value, length, nextDelimPos);
        } else {
            write(value, 0, length);
        }

        if (needsTextDelimiter) {
            write(textDelimiter);
        }
    }

    @SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:ParameterAssignment"})
    private void appendEscaped(final String value, final int length, int nextDelimPos) {
        int startPos = 0;
        do {
            final int len = nextDelimPos - startPos + 1;
            write(value, startPos, len);
            write(textDelimiter);
            startPos += len;

            nextDelimPos = -1;
            for (int i = startPos; i < length; i++) {
                if (value.charAt(i) == textDelimiter) {
                    nextDelimPos = i;
                    break;
                }
            }
        } while (nextDelimPos > -1);

        if (length > startPos) {
            write(value, startPos, length - startPos);
        }
    }

    /**
     * Appends a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to append ({@code null} values are handled as empty strings)
     * @throws UncheckedIOException if a write error occurs
     */
    public void appendLine(final String... values) {
        for (final String value : values) {
            appendField(value);
        }
        endLine();
    }

    /**
     * Appends new line character(s) to the current line.
     *
     * @throws UncheckedIOException if a write error occurs
     */
    public void endLine() {
        write(lineDelimiter, 0, lineDelimiter.length());
        newline = true;
    }

    private void write(final String value, final int off, final int length) {
        try {
            writer.write(value, off, length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void write(final char c) {
        try {
            writer.write(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        writer.flush();
    }

}
