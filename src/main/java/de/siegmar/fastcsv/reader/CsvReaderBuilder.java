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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This builder is used to create configured instances of {@link CsvReader}. The default
 * configuration of this class complies with RFC 4180.
 *
 * @author Oliver Siegmar
 */
public class CsvReaderBuilder {

    /**
     * Field separator character (default: ',' - comma).
     */
    private char fieldSeparator = ',';

    /**
     * Text delimiter character (default: '"' - double quotes).
     */
    private char textDelimiter = '"';

    /**
     * Read first line as header line? (default: false).
     */
    private boolean containsHeader;

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
     * @param textDelimiter the text delimiter character (default: '"' - double quotes).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder textDelimiter(final char textDelimiter) {
        this.textDelimiter = textDelimiter;
        return this;
    }

    /**
     * @param containsHeader if the first line should be the header (default: false).
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvReaderBuilder containsHeader(final boolean containsHeader) {
        this.containsHeader = containsHeader;
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
     * Reads an entire file and returns a CsvContainer containing the data.
     *
     * @param path    the file to read data from.
     * @param charset the character set to use - must not be {@code null}.
     * @return the entire file's data - never {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public CsvContainer read(final Path path, final Charset charset) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(charset, "charset must not be null");
        try (Reader reader = newPathReader(path, charset)) {
            return read(reader);
        }
    }

    private static Reader newPathReader(final Path path, final Charset charset) throws IOException {
        return new InputStreamReader(Files.newInputStream(path), charset);
    }

    /**
     * Reads from the provided reader until the end and returns a CsvContainer containing the data.
     * <p>
     * This library uses built-in buffering, so you do not need to pass in a buffered Reader
     * implementation such as {@link java.io.BufferedReader}.
     * Performance may be even likely better if you do not.
     *
     * @param reader the data source to read from.
     * @return the entire file's data - never {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public CsvContainer read(final Reader reader) throws IOException {
        final CsvReader csvParser =
            parse(Objects.requireNonNull(reader, "reader must not be null"));

        final List<CsvRow> rows = new ArrayList<>();
        CsvRow csvRow;
        while ((csvRow = csvParser.nextRow()) != null) {
            rows.add(csvRow);
        }

        final List<String> header = containsHeader ? csvParser.getHeader() : null;
        return new CsvContainer(header, rows);
    }

    /**
     * Constructs a new {@link CsvReader} for the specified arguments.
     *
     * @param path    the file to read data from.
     * @param charset the character set to use - must not be {@code null}.
     * @return a new CsvReader - never {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public CsvReader parse(final Path path, final Charset charset) throws IOException {
        return parse(newPathReader(
            Objects.requireNonNull(path, "path must not be null"),
            Objects.requireNonNull(charset, "charset must not be null")
        ));
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
    public CsvReader parse(final Reader reader) {
        return new CsvReader(Objects.requireNonNull(reader, "reader must not be null"),
            fieldSeparator, textDelimiter, containsHeader, skipEmptyRows,
            errorOnDifferentFieldCount);
    }

}
