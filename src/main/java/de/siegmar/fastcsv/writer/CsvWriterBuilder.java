package de.siegmar.fastcsv.writer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This builder is used to create configured instances of {@link CsvWriter}. The default
 * configuration of this class complies with RFC 4180.
 */
public final class CsvWriterBuilder {

    /**
     * Field separator character (default: ',' - comma).
     */
    private char fieldSeparator = ',';

    /**
     * The character for enclosing fields (default: '"' - double quotes).
     */
    private char quoteCharacter = '"';

    /**
     * The strategy when fields should be enclosed by the {@link #quoteCharacter}
     * (default: {@link QuoteStrategy#REQUIRED}).
     */
    private QuoteStrategy quoteStrategy = QuoteStrategy.REQUIRED;

    /**
     * The line delimiter to be used (default: {@link LineDelimiter#CRLF}).
     */
    private LineDelimiter lineDelimiter = LineDelimiter.CRLF;

    CsvWriterBuilder() {
    }

    /**
     * Sets the character that is used to separate columns (default: ',' - comma).
     *
     * @param fieldSeparator the field separator character.
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder fieldSeparator(final char fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
        return this;
    }

    /**
     * Sets the character that is used to quote values (default: '"' - double quotes).
     *
     * @param quoteCharacter the character for enclosing fields.
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder quoteCharacter(final char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
        return this;
    }

    /**
     * Sets the strategy that defines when quoting has to be performed
     * (default: {@link QuoteStrategy#REQUIRED}).
     *
     * @param quoteStrategy the strategy when fields should be enclosed using the.
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder quoteStrategy(final QuoteStrategy quoteStrategy) {
        this.quoteStrategy = quoteStrategy;
        return this;
    }

    /**
     * Sets the delimiter that is used to separate lines (default: {@link LineDelimiter#CRLF}).
     *
     * @param lineDelimiter the line delimiter to be used.
     * @return This updated object, so that additional method calls can be chained together.
     */
    public CsvWriterBuilder lineDelimiter(final LineDelimiter lineDelimiter) {
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
    public CsvWriter build(final Writer writer) {
        Objects.requireNonNull(writer, "writer must not be null");

        return new CsvWriter(writer, fieldSeparator, quoteCharacter, quoteStrategy,
            lineDelimiter, true);
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
    public CsvWriter build(final Path path, final Charset charset,
                           final OpenOption... openOptions)
        throws IOException {

        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(charset, "charset must not be null");

        return new CsvWriter(new OutputStreamWriter(Files.newOutputStream(path, openOptions),
            charset),
            fieldSeparator, quoteCharacter, quoteStrategy, lineDelimiter, false);
    }

}
