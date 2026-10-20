package net.ena.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerToggleManagerTest {
    @TempDir
    Path directory;
    private final UUID player = UUID.fromString("7e5cdd88-4567-4d41-90ba-fc99bb345fcb");

    @ParameterizedTest
    @ValueSource(strings = {"{broken", "", "null", "[]", "{\"disabledPlayers\":null}",
            "{\"disabledPlayers\":{}}"})
    void corruptFilesCannotCrashOrBeOverwritten(String invalid) throws Exception {
        Path path = directory.resolve("players.json");
        Files.writeString(path, invalid);
        PlayerToggleManager manager = new PlayerToggleManager(path);
        assertFalse(manager.load());
        assertFalse(manager.setEnabled(player, false));
        assertEquals(invalid, Files.readString(path));
    }

    @Test
    void skipsInvalidEntriesAndKeepsValidPlayers() throws Exception {
        Path path = directory.resolve("players.json");
        Files.writeString(path, "{\"disabledPlayers\":[null,12,{},\"bad\",\"" + player + "\"]}");
        PlayerToggleManager manager = new PlayerToggleManager(path);
        assertTrue(manager.load());
        assertFalse(manager.isEnabled(player));
    }

    @Test
    void preservesLoadedPreferencesOnFailureAndRecoversAfterRepair() throws Exception {
        Path path = directory.resolve("players.json");
        PlayerToggleManager manager = new PlayerToggleManager(path);
        assertTrue(manager.load());
        assertTrue(manager.setEnabled(player, false));
        Files.writeString(path, "{broken");
        assertFalse(manager.load());
        assertFalse(manager.isEnabled(player));
        assertFalse(manager.setEnabled(player, true));
        assertFalse(manager.isEnabled(player));
        Files.writeString(path, "{\"disabledPlayers\":[\"" + player + "\"]}");
        assertTrue(manager.setEnabled(player, true));
        PlayerToggleManager restored = new PlayerToggleManager(path);
        assertTrue(restored.load());
        assertTrue(restored.isEnabled(player));
    }

    @Test
    void unreadablePathDoesNotChangeInMemoryToggle() throws Exception {
        Path path = directory.resolve("players.json");
        PlayerToggleManager manager = new PlayerToggleManager(path);
        assertTrue(manager.load());
        Files.delete(path);
        Files.createDirectory(path);
        Files.writeString(path.resolve("keep.txt"), "occupied");
        assertFalse(manager.setEnabled(player, false));
        assertTrue(manager.isEnabled(player));
        assertEquals("occupied", Files.readString(path.resolve("keep.txt")));
    }

    @Test
    void corruptionAfterStartupIsNotOverwrittenByACommand() throws Exception {
        Path path = directory.resolve("players.json");
        PlayerToggleManager manager = new PlayerToggleManager(path);
        assertTrue(manager.load());
        Files.writeString(path, "{broken");
        assertFalse(manager.setEnabled(player, false));
        assertTrue(manager.isEnabled(player));
        assertEquals("{broken", Files.readString(path));
    }
}
