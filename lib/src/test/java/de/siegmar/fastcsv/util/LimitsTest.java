package de.siegmar.fastcsv.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LimitsTest {

    public static final String FASTCSV_MAX_FIELD_SIZE = "fastcsv.max.field.size";
    public static final String FASTCSV_MAX_FIELD_COUNT = "fastcsv.max.field.count";

    @AfterEach
    void cleanup() {
        System.clearProperty(FASTCSV_MAX_FIELD_SIZE);
        System.clearProperty(FASTCSV_MAX_FIELD_COUNT);
    }

    @SuppressWarnings("removal")
    @Test
    void defaultMaxFieldSize() {
        assertThat(Limits.MAX_FIELD_SIZE)
            .as("Default max field size should be correct")
            .isEqualTo(16 * 1024 * 1024);
    }

    @SuppressWarnings({"removal", "PMD.UseUnderscoresInNumericLiterals"})
    @Test
    void customMaxFieldSize() {
        System.setProperty(FASTCSV_MAX_FIELD_SIZE, "100000");

        assertThat(Limits.getIntProperty(FASTCSV_MAX_FIELD_SIZE, 16 * 1024 * 1024))
            .as("Custom max field size should be respected")
            .isEqualTo(100000);
    }

    @SuppressWarnings("removal")
    @Test
    void defaultMaxFieldCount() {
        assertThat(Limits.MAX_FIELD_COUNT)
            .as("Default max field count should be correct")
            .isEqualTo(16 * 1024);
    }

    @SuppressWarnings("removal")
    @Test
    void customMaxFieldCount() {
        System.setProperty(FASTCSV_MAX_FIELD_COUNT, "200");

        assertThat(Limits.getIntProperty(FASTCSV_MAX_FIELD_COUNT, 16 * 1024))
            .as("Custom max field count should be respected")
            .isEqualTo(200);
    }

    @SuppressWarnings("removal")
    @Test
    void invalidMaxFieldSizeThrowsException() {
        System.setProperty(FASTCSV_MAX_FIELD_SIZE, "invalid");

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> Limits.getIntProperty(FASTCSV_MAX_FIELD_SIZE, 16 * 1024 * 1024))
            .withMessageContaining("Invalid format for system property " + FASTCSV_MAX_FIELD_SIZE);
    }

    @SuppressWarnings({"removal", "PMD.UseUnderscoresInNumericLiterals"})
    @Test
    void maxRecordSizeBasedOnMaxFieldSize() {
        System.setProperty(FASTCSV_MAX_FIELD_SIZE, "4000000");

        assertThat(4 * Limits.getIntProperty(FASTCSV_MAX_FIELD_SIZE, 16 * 1024 * 1024))
            .as("MAX_RECORD_SIZE should be four times MAX_FIELD_SIZE")
            .isEqualTo(4 * 4000000);
    }
}
