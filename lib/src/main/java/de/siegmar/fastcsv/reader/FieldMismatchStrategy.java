package de.siegmar.fastcsv.reader;

/// Defines the strategy for handling records with a field count that does not match the first record.
///
/// @see CsvReader.CsvReaderBuilder#extraFieldStrategy(FieldMismatchStrategy)
/// @see CsvReader.CsvReaderBuilder#missingFieldStrategy(FieldMismatchStrategy)
public enum FieldMismatchStrategy {

    /// Throw a [CsvParseException] when a field count mismatch is detected. This is the default behavior.
    STRICT,

    /// Allow the record as-is, without any modification.
    IGNORE,

    /// Silently drop the record.
    SKIP

}
