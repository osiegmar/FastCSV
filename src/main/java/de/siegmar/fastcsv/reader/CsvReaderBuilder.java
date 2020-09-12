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

package de.siegmar.fastcsv.reader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This builder is used to create configured instances of {@link CsvReader}. The default
 * configuration of this class complies with RFC 4180.
 *
 * @author Oliver Siegmar
 */
public final class CsvReaderBuilder {

    /**
     * Field separator character (default: ',' - comma).
     */
    private char fieldSeparator = ',';

    /**
     * The character used to enclose fields (default: '"' - double quotes).
     */
    private char quoteCharacter = '"';

    /**
     * Skip empty rows? (default: true)
     */
    private boolean skipEmptyRows = true;

    /**
     * Throw an exception if CSV data contains different field count? (default: false).
     */
    private boolean errorOnDifferentFieldCount;

    /**
     * @param fieldSeparator the field separator character (default: ',' - comma).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder fieldSeparator(final char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
        return this;
    }

    /**
     * @param quoteCharacter the character used to enclose fields (default: '"' - double quotes).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder quoteCharacter(final char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
        return this;
    }

    /**
     * @param skipEmptyRows if empty rows should be skipped (default: true).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder skipEmptyRows(final boolean skipEmptyRows) {
        this.skipEmptyRows = skipEmptyRows;
        return this;
    }

    /**
     * @param errorOnDifferentFieldCount if an exception should be thrown, if CSV data contains
     *                                   different field count (default: false).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder errorOnDifferentFieldCount(final boolean errorOnDifferentFieldCount) {
        this.errorOnDifferentFieldCount = errorOnDifferentFieldCount;
        return this;
    }

    /**
     * Constructs a new {@link CsvReader} for the specified arguments.
     *
     * @param path    the file to read data from.
     * @param charset the character set to use - must not be {@code null}.
     * @return a new CsvReader - never {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public CsvReader build(final Path path, final Charset charset) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        return new CsvReader(newPathReader(path, charset),
            fieldSeparator, quoteCharacter, skipEmptyRows, errorOnDifferentFieldCount);
    }

    /**
     * Constructs a new {@link CsvReader} for the specified arguments.
     * <p>
     * This library uses built-in buffering, so you do not need to pass in a buffered Reader
     * implementation such as {@link java.io.BufferedReader}.
     * Performance may be even likely better if you do not.
     *
     * @param reader the data source to read from.
     * @return a new CsvReader - never {@code null}.
     */
    public CsvReader build(final Reader reader) {
        return new CsvReader(Objects.requireNonNull(reader, "reader must not be null"),
            fieldSeparator, quoteCharacter, skipEmptyRows, errorOnDifferentFieldCount);
    }

    private static Reader newPathReader(final Path path, final Charset charset) throws IOException {
        return new InputStreamReader(Files.newInputStream(path), charset);
    }

}
