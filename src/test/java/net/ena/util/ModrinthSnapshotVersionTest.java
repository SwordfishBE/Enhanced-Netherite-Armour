package net.ena.util;

import net.minecraft.SharedConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModrinthSnapshotVersionTest {
    @Test
    void usesRawMinecraftIdInsteadOfTheLoaderAlphaAlias() {
        SharedConstants.tryDetectVersion();
        assertEquals("26.4-snapshot-6", ModrinthUpdateChecker.currentMinecraftVersion());
    }
}
