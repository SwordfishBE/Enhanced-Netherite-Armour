package net.ena.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ClothConfigCompatibilityTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "unknown", "26.1.154", "26.3.159"})
    void rejectsMissingUnrecognizedAndOldVersions(String version) {
        assertFalse(ClothConfigCompatibility.isCompatible(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26.4-alpha.1", "26.4", "26.4.160", "26.5.1"})
    void acceptsVersionsAtThePreparedApiBoundary(String version) {
        assertTrue(ClothConfigCompatibility.isCompatible(version));
    }
}
