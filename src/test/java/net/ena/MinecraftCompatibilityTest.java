package net.ena;

import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.game.minecraft.McVersionLookup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftCompatibilityTest {
    @ParameterizedTest
    @ValueSource(strings = {"26.4-snapshot-6", "26.4-snapshot-7", "26.4-snapshot-10",
            "26.4-pre-1", "26.4-rc-1", "26.4"})
    void acceptsSnapshotSixThroughFinal(String version) throws Exception {
        assertTrue(supports(version), version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"26.3", "26.4-snapshot-1", "26.4-snapshot-5",
            "26.4.1-pre-1", "26.4.1", "26.5-snapshot-1", "26.5", "27.1"})
    void rejectsVersionsOutsideTheRequestedRange(String version) throws Exception {
        assertFalse(supports(version), version);
    }

    private static boolean supports(String id) throws Exception {
        Version normalized = Version.parse(McVersionLookup.normalizeVersion(id, McVersionLookup.getRelease(id)));
        try (var input = Objects.requireNonNull(MinecraftCompatibilityTest.class.getResourceAsStream("/fabric.mod.json"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            String range = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("depends").get("minecraft").getAsString();
            return VersionPredicate.parse(range).test(normalized);
        }
    }
}
