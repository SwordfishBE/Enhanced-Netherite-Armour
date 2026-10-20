package net.ena.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import net.ena.EnhancedNetheriteArmour;
import net.ena.util.AtomicFiles;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerToggleManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.STRICT).create();
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedNetheriteArmour.MOD_ID);
    private final Path filePath;
    private final Set<UUID> disabledPlayers = new LinkedHashSet<>();

    public PlayerToggleManager() {
        this(FabricLoader.getInstance().getConfigDir().resolve("enhancednetheritearmour-player-settings.json"));
    }

    PlayerToggleManager(Path filePath) {
        this.filePath = filePath;
    }

    public boolean load() {
        try {
            Set<UUID> loaded = new LinkedHashSet<>();
            if (Files.notExists(filePath)) {
                if (!save(loaded)) {
                    return false;
                }
            } else {
                JsonElement root = GSON.fromJson(Files.readString(filePath), JsonElement.class);
                if (root == null || !root.isJsonObject()) {
                    throw new JsonParseException("Player settings must be a JSON object.");
                }
                JsonElement entries = root.getAsJsonObject().get("disabledPlayers");
                if (entries == null || !entries.isJsonArray()) {
                    throw new JsonParseException("disabledPlayers must be an array.");
                }
                for (JsonElement entry : entries.getAsJsonArray()) {
                    if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                        LOGGER.warn("[EnhancedNetheriteArmour] Ignoring a non-string player UUID in saved toggles.");
                        continue;
                    }
                    try {
                        loaded.add(UUID.fromString(entry.getAsString()));
                    } catch (IllegalArgumentException exception) {
                        LOGGER.warn("[EnhancedNetheriteArmour] Ignoring invalid player UUID '{}' in saved toggles.",
                                entry.getAsString());
                    }
                }
            }
            disabledPlayers.clear();
            disabledPlayers.addAll(loaded);
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[EnhancedNetheriteArmour] Failed to load player settings; leaving the file and current preferences untouched.",
                    exception);
            return false;
        }
    }

    public boolean isEnabled(UUID playerId) {
        return !disabledPlayers.contains(playerId);
    }

    public boolean setEnabled(UUID playerId, boolean enabled) {
        // Honor administrator edits and refuse to overwrite a file corrupted since startup.
        if (!load()) {
            return false;
        }
        Set<UUID> updated = new LinkedHashSet<>(disabledPlayers);
        boolean changed = enabled ? updated.remove(playerId) : updated.add(playerId);
        if (!changed) {
            return true;
        }
        if (!save(updated)) {
            return false;
        }
        disabledPlayers.clear();
        disabledPlayers.addAll(updated);
        return true;
    }

    private boolean save(Set<UUID> preferences) {
        JsonObject root = new JsonObject();
        root.add("disabledPlayers", GSON.toJsonTree(preferences.stream().map(UUID::toString).toList()));
        try {
            AtomicFiles.writeString(filePath, GSON.toJson(root));
            return true;
        } catch (IOException exception) {
            LOGGER.warn("[EnhancedNetheriteArmour] Failed to save player settings; keeping current preferences.", exception);
            return false;
        }
    }
}
