/// Provides classes for reading CSV data.
///
/// The main entry point is [de.siegmar.fastcsv.reader.CsvReader], which reads CSV records sequentially
/// using an iterator or stream. Records are represented as [de.siegmar.fastcsv.reader.CsvRecord]
/// (index-based access) or [de.siegmar.fastcsv.reader.NamedCsvRecord] (header-based access).
///
/// For random access to large CSV files, use [de.siegmar.fastcsv.reader.IndexedCsvReader], which builds
/// a [de.siegmar.fastcsv.reader.CsvIndex] for page-based retrieval.
///
/// Custom record types can be built by implementing [de.siegmar.fastcsv.reader.CsvCallbackHandler].
///
/// All readers implement [java.io.Closeable] and should be used in a try-with-resources block.
///
/// Obtain a reader via [de.siegmar.fastcsv.reader.CsvReader#builder()].
package de.siegmar.fastcsv.reader;
