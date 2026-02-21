/// Provides classes for writing CSV data.
///
/// The main entry point is [de.siegmar.fastcsv.writer.CsvWriter], configured via its builder.
/// It supports writing complete records, field-by-field record construction
/// (via [de.siegmar.fastcsv.writer.CsvWriter.CsvWriterRecord]), and comment lines.
///
/// Quoting behavior can be customized using [de.siegmar.fastcsv.writer.QuoteStrategy]
/// (see [de.siegmar.fastcsv.writer.QuoteStrategies] for built-in strategies).
///
/// The writer implements [java.io.Closeable] and [java.io.Flushable] and should be used
/// in a try-with-resources block to ensure all buffered data is written.
///
/// Obtain a writer via [de.siegmar.fastcsv.writer.CsvWriter#builder()].
package de.siegmar.fastcsv.writer;
