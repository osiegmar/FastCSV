package de.siegmar.fastcsv.util;

/// The `Limits` class defines the maximum limits for various fields and records in a CSV file.
///
/// @deprecated This class is deprecated and will be removed in a future release.
///     See individual constants for alternatives.
@Deprecated(since = "3.6.0", forRemoval = true)
public final class Limits {

    /// The `MAX_FIELD_SIZE` constant defines the maximum size for a single field in a CSV file.
    /// The value is set to 16,777,216 characters (16 to 64 MiB depending on the circumstance of multibyte character
    /// utilization).
    ///
    /// The default value can be overridden by setting the system property {@systemProperty fastcsv.max.field.size}
    /// (e.g., using `-Dfastcsv.max.field.size=8388608`).
    ///
    /// @deprecated Use [de.siegmar.fastcsv.reader.CsvReader.CsvReaderBuilder#maxBufferSize(int)],
    ///     [de.siegmar.fastcsv.reader.CsvRecordHandler.CsvRecordHandlerBuilder#maxFieldSize(int)],
    ///     [de.siegmar.fastcsv.reader.NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#maxFieldSize(int)],
    ///     or [de.siegmar.fastcsv.reader.StringArrayHandler.StringArrayHandlerBuilder#maxFieldSize(int)] instead.
    public static final int MAX_FIELD_SIZE = getIntProperty("fastcsv.max.field.size", 16 * 1024 * 1024);

    /// The `MAX_FIELD_COUNT` constant defines the maximum number of fields per record.
    /// The value is set to 16,384.
    ///
    /// The default value can be overridden by setting the system property {@systemProperty fastcsv.max.field.count}
    /// (e.g., using `-Dfastcsv.max.field.count=8192`).
    ///
    /// @deprecated Use [de.siegmar.fastcsv.reader.CsvRecordHandler.CsvRecordHandlerBuilder#maxFields(int)],
    ///     [de.siegmar.fastcsv.reader.NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#maxFields(int)],
    ///     or [de.siegmar.fastcsv.reader.StringArrayHandler.StringArrayHandlerBuilder#maxFields(int)] instead.
    public static final int MAX_FIELD_COUNT = getIntProperty("fastcsv.max.field.count", 16 * 1024);

    /// The `MAX_RECORD_SIZE` constant defines the maximum size for all fields combined in a CSV record.
    /// The value is set to four times of [#MAX_FIELD_SIZE].
    ///
    /// @deprecated Use [de.siegmar.fastcsv.reader.CsvRecordHandler.CsvRecordHandlerBuilder#maxRecordSize(int)],
    ///     [de.siegmar.fastcsv.reader.NamedCsvRecordHandler.NamedCsvRecordHandlerBuilder#maxRecordSize(int)],
    ///     or [de.siegmar.fastcsv.reader.StringArrayHandler.StringArrayHandlerBuilder#maxRecordSize(int)] instead.
    public static final int MAX_RECORD_SIZE = 4 * MAX_FIELD_SIZE;

    private Limits() {
    }

    /// Retrieves the system property value if presented, otherwise the default value is returned.
    /// If the property cannot be parsed as an integer, an [IllegalArgumentException] is thrown.
    ///
    /// @param key          The system property key.
    /// @param defaultValue The default value to use if the system property is not set or is invalid.
    /// @return The system property value as an integer or the default value if the property is not set or is invalid.
    /// @throws IllegalArgumentException If the system property value cannot be parsed as an integer.
    static int getIntProperty(final String key, final int defaultValue) {
        final String value = System.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for system property " + key, e);
        }
    }

}
