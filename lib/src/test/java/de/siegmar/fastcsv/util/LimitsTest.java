package de.siegmar.fastcsv.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LimitsTest {

    @BeforeEach
    void setup() {
        System.clearProperty("fastcsv.max.field.size");
        System.clearProperty("fastcsv.max.field.count");
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("fastcsv.max.field.size");
        System.clearProperty("fastcsv.max.field.count");
    }

    @Test
    void defaultMaxFieldSize() {
        assertEquals(16 * 1024 * 1024, Limits.MAX_FIELD_SIZE, "Default max field size should be correct");
    }

    @Test
    void customMaxFieldSize() {
        System.setProperty("fastcsv.max.field.size", "100000");

        assertEquals(100000, Limits.getIntProperty("fastcsv.max.field.size", 16 * 1024 * 1024),
                "Custom max field size should be respected");
    }

    @Test
    void defaultMaxFieldCount() {
        assertEquals(16 * 1024, Limits.MAX_FIELD_COUNT, "Default max field count should be correct");
    }

    @Test
    void customMaxFieldCount() {
        System.setProperty("fastcsv.max.field.count", "200");

        assertEquals(200, Limits.getIntProperty("fastcsv.max.field.count", 16 * 1024),
                "Custom max field count should be respected");
    }

    @Test
    void invalidMaxFieldSizeThrowsException() {
        System.setProperty("fastcsv.max.field.size", "invalid");

        assertThrows(IllegalArgumentException.class,
                () -> Limits.getIntProperty("fastcsv.max.field.size", 16 * 1024 * 1024),
                "Should throw IllegalArgumentException for invalid integer format");
    }

    @Test
    void testMaxRecordSizeBasedOnMaxFieldSize() {
        System.setProperty("fastcsv.max.field.size", "4000000");

        assertEquals(4 * 4000000,
                4 * Limits.getIntProperty("fastcsv.max.field.size", 16 * 1024 * 1024),
                "MAX_RECORD_SIZE should be four times MAX_FIELD_SIZE");
    }
}
