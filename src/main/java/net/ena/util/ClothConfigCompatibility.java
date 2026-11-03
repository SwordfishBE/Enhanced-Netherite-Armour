package net.ena.util;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public final class ClothConfigCompatibility {
    private ClothConfigCompatibility() {
    }

    public static boolean isCompatible(String version) {
        if (version == null) {
            return false;
        }
        try {
            return VersionPredicate.parse(">=26.4-").test(SemanticVersion.parse(version));
        } catch (VersionParsingException exception) {
            return false;
        }
    }
}
