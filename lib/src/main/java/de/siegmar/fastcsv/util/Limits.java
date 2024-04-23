package de.siegmar.fastcsv.util;

/**
 * The {@code Limits} class defines the maximum limits for various fields and records in a CSV file.
 * <p>
 * Properties can be overridden by using
 * <p>
 * Example use:
 * {@snippet : System.setProperty("fastcsv.max.field.size", "1024"); }
 * <p>
 * Or using VM options: {@snippet : -Dfastcsv.max.field.count=1024 }
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
     * Retrieves the system property value as an integer, with a fallback to a default value.
     * If the property is not set or cannot be parsed, the default value is returned.
     *
     * @param key The system property key.
     * @param defaultValue The default value to use if the system property is not set or is invalid.
     * @return The system property value as an integer or the default value if the property is not set or is invalid.
     */
    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid format for system property " + key);
                throw e;
            }
        }
        return defaultValue;
    }
}
