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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Objects;

/**
 * This is the main class for writing CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvWriter {

    /**
     * Field separator character (default: ',' - comma).
     */
    private char fieldSeparator = ',';

    /**
     * Text delimiter character (default: '"' - double quotes).
     */
    private char textDelimiter = '"';

    /**
     * Should fields always delimited using the {@link #textDelimiter}? (default: false).
     */
    private boolean alwaysDelimitText;

    /**
     * The line delimiter character(s) to be used (default: {@link System#lineSeparator()}).
     */
    private char[] lineDelimiter = System.lineSeparator().toCharArray();

    /**
     * Sets the field separator character (default: ',' - comma).
     */
    public void setFieldSeparator(final char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    /**
     * Sets the text delimiter character (default: '"' - double quotes).
     */
    public void setTextDelimiter(final char textDelimiter) {
        this.textDelimiter = textDelimiter;
    }

    /**
     * Sets if fields should always delimited using the {@link #textDelimiter} (default: false).
     */
    public void setAlwaysDelimitText(final boolean alwaysDelimitText) {
        this.alwaysDelimitText = alwaysDelimitText;
    }

    /**
     * Sets the line delimiter character(s) to be used (default: {@link System#lineSeparator()}).
     */
    public void setLineDelimiter(final char[] lineDelimiter) {
        this.lineDelimiter = lineDelimiter.clone();
    }

    /**
     * Writes all specified data to the file.
     *
     * @param file where the data should be written to.
     * @param data lines/columns to be written.
     * @throws IOException if a write error occurs
     * @throws NullPointerException if file, charset or data is null
     */
    public void write(final File file, final Charset charset, final Collection<String[]> data)
        throws IOException {

        write(
            Objects.requireNonNull(file, "file must not be null").toPath(),
            Objects.requireNonNull(charset, "charset must not be null"),
            data
        );
    }

    /**
     * Writes all specified data to the path.
     *
     * @param path where the data should be written to.
     * @param data lines/columns to be written.
     * @throws IOException if a write error occurs
     * @throws NullPointerException if path, charset or data is null
     */
    public void write(final Path path, final Charset charset, final Collection<String[]> data)
        throws IOException {

        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(charset, "charset must not be null");
        try (final Writer writer = newWriter(path, charset)) {
            write(writer, data);
        }
    }

    /**
     * Writes all specified data to the writer.
     *
     * @param writer where the data should be written to.
     * @param data lines/columns to be written.
     * @throws IOException if a write error occurs
     * @throws NullPointerException if writer or data is null
     */
    public void write(final Writer writer, final Collection<String[]> data) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        final CsvAppender appender = append(writer);
        for (final String[] values : data) {
            appender.appendLine(values);
        }
        appender.flush();
    }

    /**
     * Constructs a {@link CsvAppender} for the specified File.
     *
     * @param file the file to write data to.
     * @param charset the character set to be used for writing data to the file.
     * @return a new CsvAppender instance
     * @throws IOException if a write error occurs
     * @throws NullPointerException if file or charset is null
     */
    public CsvAppender append(final File file, final Charset charset) throws IOException {
        return append(
            Objects.requireNonNull(file, "file must not be null").toPath(),
            Objects.requireNonNull(charset, "charset must not be null")
        );
    }

    /**
     * Constructs a {@link CsvAppender} for the specified Path.
     *
     * @param path the Path (file) to write data to.
     * @param charset the character set to be used for writing data to the file.
     * @return a new CsvAppender instance
     * @throws IOException if a write error occurs
     * @throws NullPointerException if path or charset is null
     */
    public CsvAppender append(final Path path, final Charset charset) throws IOException {
        return append(newWriter(
            Objects.requireNonNull(path, "path must not be null"),
            Objects.requireNonNull(charset, "charset must not be null")
        ));
    }

    /**
     * Constructs a {@link CsvAppender} for the specified Writer.
     *
     * This library uses built-in buffering, so you do not need to pass in a buffered Writer
     * implementation such as {@link java.io.BufferedWriter}.
     * Performance may be even likely better if you do not.
     *
     * @param writer the Writer to use for writing CSV data.
     * @return a new CsvAppender instance
     * @throws NullPointerException if writer is null
     */
    public CsvAppender append(final Writer writer) {
        return new CsvAppender(Objects.requireNonNull(writer, "writer must not be null"),
            fieldSeparator, textDelimiter, alwaysDelimitText, lineDelimiter);
    }

    private static Writer newWriter(final Path path, final Charset charset) throws IOException {
        return new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING), charset);
    }

}
