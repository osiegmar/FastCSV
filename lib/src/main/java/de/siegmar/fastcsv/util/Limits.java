package de.siegmar.fastcsv.util;

/**
 * The {@code Limits} class defines the maximum limits for various fields and records in a CSV file.
 */
public final class Limits {

    /**
     * The {@code MAX_FIELD_SIZE} constant defines the maximum size for a single field in a CSV file.
     * The value is set to 16,777,216 characters (16 to 64 MiB depending on the circumstance of multibyte character
     * utilization).
     */
    public static final int MAX_FIELD_SIZE = 16 * 1024 * 1024;

    /**
     * The {@code MAX_FIELDS_SIZE} constant defines the maximum number of fields per record.
     * The value is set to 16,384.
     */
    public static final int MAX_FIELD_COUNT = 16 * 1024;

    /**
     * The {@code MAX_RECORD_SIZE} constant defines the maximum size for all fields combined in a CSV record.
     * The value is set to four times of {@code MAX_FIELD_SIZE}.
     */
    public static final int MAX_RECORD_SIZE = 4 * MAX_FIELD_SIZE;

    private Limits() {
    }

}
