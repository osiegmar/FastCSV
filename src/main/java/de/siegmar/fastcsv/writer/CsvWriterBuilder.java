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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This is the main class for writing CSV data.
 *
 * @author Oliver Siegmar
 */
public final class CsvWriterBuilder {

    /**
     * Field separator character (default: ',' - comma).
     */
    private char fieldSeparator = ',';

    /**
     * Text delimiter character (default: '"' - double quotes).
     */
    private char textDelimiter = '"';

    /**
     * The strategy when fields should be delimited using the {@link #textDelimiter}
     * (default: {@link TextDelimitStrategy#REQUIRED}).
     */
    private TextDelimitStrategy textDelimitStrategy = TextDelimitStrategy.REQUIRED;

    /**
     * The line delimiter character(s) to be used (default: CRLF as defined in RFC 4180).
     */
    private String lineDelimiter = "\r\n";

    CsvWriterBuilder() {
    }

    /**
     * @param fieldSeparator the field separator character.
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder fieldSeparator(final char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
        return this;
    }

    /**
     * @param textDelimiter the text delimiter character (default: '"' - double quotes).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder textDelimiter(final char textDelimiter) {
        this.textDelimiter = textDelimiter;
        return this;
    }

    /**
     * @param textDelimitStrategy the strategy when fields should be delimited using the
     *                            {@link #textDelimiter}
     *                            (default: {@link TextDelimitStrategy#REQUIRED}).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder textDelimitStrategy(final TextDelimitStrategy textDelimitStrategy) {
        this.textDelimitStrategy = textDelimitStrategy;
        return this;
    }

    /**
     * @param lineDelimiter the line delimiter string to be used
     *                      (default: {@link System#lineSeparator()}).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder lineDelimiter(final String lineDelimiter) {
        this.lineDelimiter = lineDelimiter;
        return this;
    }

    /**
     * Constructs a {@link CsvWriter} for the specified Writer.
     *
     * @param writer the Writer to use for writing CSV data.
     * @return a new CsvWriter instance
     * @throws NullPointerException if writer is null
     */
    public CsvWriter to(final Writer writer) {
        Objects.requireNonNull(writer, "writer must not be null");

        return new CsvWriter(writer, fieldSeparator, textDelimiter, textDelimitStrategy,
            lineDelimiter);
    }

    /**
     * Constructs a {@link CsvWriter} for the specified Path.
     *
     * @param path        the Path (file) to write data to.
     * @param charset     the character set to be used for writing data to the file.
     * @param openOptions options specifying how the file is opened.
     *                    See {@link Files#newOutputStream(Path, OpenOption...)} for defaults.
     * @return a new CsvWriter instance
     * @throws IOException          if a write error occurs
     * @throws NullPointerException if path or charset is null
     */
    public CloseableCsvWriter to(final Path path, final Charset charset,
                                 final OpenOption... openOptions)
        throws IOException {

        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        return new CloseableCsvWriter(fastBuffer(Files.newOutputStream(path, openOptions), charset),
            fieldSeparator, textDelimiter, textDelimitStrategy, lineDelimiter);
    }

    /**
     * Constructs a {@link CsvWriter} for the specified File.
     *
     * @param file    the file to write data to.
     * @param charset the character set to be used for writing data to the file.
     * @param append  if {@code true}, then file is opened in append mode rather overwriting it.
     * @return a new CsvWriter instance
     * @throws IOException          if a write error occurs
     * @throws NullPointerException if file or charset is null
     */
    public CloseableCsvWriter to(final File file, final Charset charset, final boolean append)
        throws IOException {

        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        return new CloseableCsvWriter(fastBuffer(new FileOutputStream(file, append), charset),
            fieldSeparator, textDelimiter, textDelimitStrategy, lineDelimiter);
    }

    private static FastBufferedWriter fastBuffer(final OutputStream out, final Charset charset) {
        return new FastBufferedWriter(new OutputStreamWriter(out, charset));
    }

}
