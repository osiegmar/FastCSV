package de.siegmar.fastcsv.util;

/**
 * The {@code Limits} class defines the maximum limits for various fields and records in a CSV file.
 * <p>
 * Example use:
 * <pre>{@code
 * System.setProperty("fastcsv.max.field.size", "1024");
 * }</pre>
 * <p>
 * Or using VM options:
 * <pre>{@code
 * -Dfastcsv.max.field.count=1024
 * }</pre>
 */
public final class Limits {

    /**
     * The {@code MAX_FIELD_SIZE} constant defines the maximum size for a single field in a CSV file.
     * The value is set to 16,777,216 characters (16 to 64 MiB depending on the circumstance of multibyte character
     * utilization).
     */
    public static final int MAX_FIELD_SIZE = getIntProperty("fastcsv.max.field.size", 16 * 1024 * 1024);

    /**
     * The {@code MAX_FIELDS_SIZE} constant defines the maximum number of fields per record.
     * The value is set to 16,384.
     */
    public static final int MAX_FIELD_COUNT = getIntProperty("fastcsv.max.field.count", 16 * 1024);

    /**
     * The {@code MAX_RECORD_SIZE} constant defines the maximum size for all fields combined in a CSV record.
     * The value is set to four times of {@code MAX_FIELD_SIZE}.
     */
    public static final int MAX_RECORD_SIZE = 4 * MAX_FIELD_SIZE;

    private Limits() {
    }

    /**
     * Retrieves the system property value if presented, otherwise default value is returned.
     * If the property cannot be parsed as an integer, an {@code IllegalArgumentException} is thrown.
     *
     * @param key The system property key.
     * @param defaultValue The default value to use if the system property is not set or is invalid.
     * @return The system property value as an integer or the default value if the property is not set or is invalid.
     * @throws IllegalArgumentException If the system property value cannot be parsed as an integer.
     */
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
