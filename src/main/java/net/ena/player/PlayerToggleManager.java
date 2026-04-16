package net.ena.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ena.EnhancedNetheriteArmour;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerToggleManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "enhancednetheritearmour-player-settings.json";

    private final Path filePath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    private final Set<UUID> disabledPlayers = new LinkedHashSet<>();

    public void load() {
        disabledPlayers.clear();

        if (!Files.exists(filePath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            StoredPreferences storedPreferences = GSON.fromJson(reader, StoredPreferences.class);
            if (storedPreferences == null || storedPreferences.disabledPlayers == null) {
                return;
            }

            for (String playerId : storedPreferences.disabledPlayers) {
                try {
                    disabledPlayers.add(UUID.fromString(playerId));
                } catch (IllegalArgumentException exception) {
                    EnhancedNetheriteArmour.LOGGER.warn("{} Ignoring invalid player UUID '{}' in saved toggles.",
                            EnhancedNetheriteArmour.prefix(), playerId);
                }
            }

            EnhancedNetheriteArmour.LOGGER.debug("{} Loaded {} player toggle entries.",
                    EnhancedNetheriteArmour.prefix(), disabledPlayers.size());
        } catch (IOException exception) {
            EnhancedNetheriteArmour.LOGGER.warn("{} Failed to load player toggle settings.",
                    EnhancedNetheriteArmour.prefix(), exception);
        }
    }

    public boolean isEnabled(UUID playerId) {
        return !disabledPlayers.contains(playerId);
    }

    public void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            disabledPlayers.remove(playerId);
        } else {
            disabledPlayers.add(playerId);
        }
        save();
    }

    private void save() {
        StoredPreferences storedPreferences = new StoredPreferences();
        for (UUID playerId : disabledPlayers) {
            storedPreferences.disabledPlayers.add(playerId.toString());
        }

        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(storedPreferences, writer);
            }
            EnhancedNetheriteArmour.LOGGER.debug("{} Saved player toggle settings to {}.",
                    EnhancedNetheriteArmour.prefix(), filePath);
        } catch (IOException exception) {
            EnhancedNetheriteArmour.LOGGER.warn("{} Failed to save player toggle settings.",
                    EnhancedNetheriteArmour.prefix(), exception);
        }
    }

    private static final class StoredPreferences {
        private final Set<String> disabledPlayers = new LinkedHashSet<>();
    }
}
