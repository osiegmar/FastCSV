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
import java.io.Writer;

/**
 * This is the main class for writing CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvAppender implements Closeable, Flushable {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private final Writer writer;
    private final char fieldSeparator;
    private final char textDelimiter;
    private final boolean alwaysDelimitText;
    private final char[] lineDelimiter;

    private boolean newline = true;

    CsvAppender(final Writer writer, final char fieldSeparator, final char textDelimiter,
                final boolean alwaysDelimitText, final char[] lineDelimiter) {
        this.writer = new FastBufferedWriter(writer);
        this.fieldSeparator = fieldSeparator;
        this.textDelimiter = textDelimiter;
        this.alwaysDelimitText = alwaysDelimitText;
        this.lineDelimiter = lineDelimiter;
    }

    /**
     * Appends a field to the current row. Automatically adds field separator and text delimiters
     * as required.
     *
     * @param value the field to append ({@code null} is handled as empty string)
     * @throws IOException if a write error occurs
     */
    public void appendField(final String value) throws IOException {
        if (!newline) {
            writer.write(fieldSeparator);
        } else {
            newline = false;
        }

        if (value == null) {
            if (alwaysDelimitText) {
                writer.write(textDelimiter);
                writer.write(textDelimiter);
            }
            return;
        }

        final char[] valueChars = value.toCharArray();
        boolean needsTextDelimiter = alwaysDelimitText;
        boolean containsTextDelimiter = false;

        for (final char c : valueChars) {
            if (c == textDelimiter) {
                containsTextDelimiter = needsTextDelimiter = true;
                break;
            } else if (c == fieldSeparator || c == LF || c == CR) {
                needsTextDelimiter = true;
            }
        }

        if (needsTextDelimiter) {
            writer.write(textDelimiter);
        }

        if (containsTextDelimiter) {
            for (final char c : valueChars) {
                if (c == textDelimiter) {
                    writer.write(textDelimiter);
                }
                writer.write(c);
            }
        } else {
            writer.write(valueChars);
        }

        if (needsTextDelimiter) {
            writer.write(textDelimiter);
        }
    }

    /**
     * Appends a complete line - one or more fields and new line character(s) at the end.
     *
     * @param values the fields to append ({@code null} values are handled as empty strings)
     * @throws IOException if a write error occurs
     */
    public void appendLine(final String... values) throws IOException {
        for (final String value : values) {
            appendField(value);
        }
        endLine();
    }

    /**
     * Appends new line character(s) to the current line.
     *
     * @throws IOException if a write error occurs
     */
    public void endLine() throws IOException {
        writer.write(lineDelimiter);
        newline = true;
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
