package net.ena.config;

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

public final class EnaConfig {

    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedNetheriteArmour.MOD_ID);

    private static volatile EnaConfig instance = disabledDefaults();

    public boolean enabled = true;
    public boolean useLuckPerms = false;
    public boolean allowPlayerToggle = true;
    public boolean armoredElytraSupport = true;
    public boolean horseFireProtection = true;
    public boolean wolfFireProtection = false;

    public static EnaConfig get() {
        return instance;
    }

    private static EnaConfig disabledDefaults() {
        EnaConfig config = new EnaConfig();
        config.enabled = false;
        return config;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("enhancednetheritearmour.json");
    }

    public static boolean load() {
        return load(configPath());
    }

    static boolean load(Path path) {
        try {
            EnaConfig loaded;
            if (Files.notExists(path)) {
                loaded = new EnaConfig();
                AtomicFiles.writeString(path, toCommentedJson(loaded));
            } else {
                loaded = parse(Files.readString(path));
            }
            instance = loaded;
            LOGGER.debug("[EnhancedNetheriteArmour] Config loaded.");
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[EnhancedNetheriteArmour] Failed to load config; keeping current settings and leaving the file untouched.",
                    exception);
            return false;
        }
    }

    static EnaConfig parse(String rawConfig) {
        JsonElement root = GSON.fromJson(stripJsonComments(rawConfig), JsonElement.class);
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException("Config must be a JSON object.");
        }
        JsonObject object = root.getAsJsonObject();
        for (String key : new String[]{"enabled", "useLuckPerms", "allowPlayerToggle",
                "armoredElytraSupport", "horseFireProtection", "wolfFireProtection"}) {
            if (object.has(key) && (!object.get(key).isJsonPrimitive()
                    || !object.getAsJsonPrimitive(key).isBoolean())) {
                throw new JsonParseException("Expected a boolean for " + key);
            }
        }
        EnaConfig config = GSON.fromJson(object, EnaConfig.class);
        config.normalize();
        return config;
    }

    public static EnaConfig loadForEditing() {
        return instance.copy();
    }

    public static void applyEditedConfig(EnaConfig editedConfig) {
        EnaConfig normalized = editedConfig == null ? new EnaConfig() : editedConfig.copy();
        normalized.normalize();
        try {
            AtomicFiles.writeString(configPath(), toCommentedJson(normalized));
            instance = normalized;
        } catch (IOException exception) {
            LOGGER.warn("[EnhancedNetheriteArmour] Failed to save config; keeping current settings.", exception);
        }
    }

    public EnaConfig copy() {
        EnaConfig copy = new EnaConfig();
        copy.enabled = enabled;
        copy.useLuckPerms = useLuckPerms;
        copy.allowPlayerToggle = allowPlayerToggle;
        copy.armoredElytraSupport = armoredElytraSupport;
        copy.horseFireProtection = horseFireProtection;
        copy.wolfFireProtection = wolfFireProtection;
        return copy;
    }

    public void normalize() {
        // Reserved for future validation.
    }

    private static String toCommentedJson(EnaConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        appendComment(sb, "Master switch for Enhanced Netherite Armour.");
        appendComment(sb, "If false, the mod stays loaded but no Fire Resistance is applied.");
        appendProperty(sb, "enabled", config.enabled, true);

        appendComment(sb, "Enable LuckPerms permission checks when the luckperms mod is installed.");
        appendComment(sb, "If false, the mod ignores LuckPerms and all players can use the feature.");
        appendProperty(sb, "useLuckPerms", config.useLuckPerms, true);

        appendComment(sb, "Allow players to toggle their own Fire Resistance support with /ena enable and /ena disable.");
        appendComment(sb, "If false, the player commands reject execution and eligible players are always treated as enabled.");
        appendProperty(sb, "allowPlayerToggle", config.allowPlayerToggle, true);

        appendComment(sb, "Enable support for Armored Elytra and the compatible datapack variant.");
        appendComment(sb, "If true, a Netherite armored elytra counts as the chest piece for the full-set check.");
        appendProperty(sb, "armoredElytraSupport", config.armoredElytraSupport, true);

        appendComment(sb, "Give Fire Resistance to horses wearing Netherite Horse Armor.");
        appendComment(sb, "This is a global server-side toggle and is not controlled per player.");
        appendProperty(sb, "horseFireProtection", config.horseFireProtection, true);

        appendComment(sb, "Protect tamed wolves of any variant from fire and help them swim in lava while wearing Wolf Armor.");
        appendComment(sb, "Disabled by default. Applies to all players' tamed wolves.");
        appendProperty(sb, "wolfFireProtection", config.wolfFireProtection, false);
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendComment(StringBuilder sb, String comment) {
        sb.append("  // ").append(comment).append('\n');
    }

    private static void appendProperty(StringBuilder sb, String key, boolean value, boolean trailingComma) {
        sb.append("  \"").append(key).append("\": ").append(value);
        if (trailingComma) {
            sb.append(',');
        }
        sb.append('\n').append('\n');
    }

    private static String stripJsonComments(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean inString = false;
        boolean escaping = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            char next = i + 1 < input.length() ? input.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    sb.append(current);
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                sb.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                sb.append(current);
                continue;
            }

            if (current == '/' && next == '/') {
                sb.append(' ');
                inLineComment = true;
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                sb.append(' ');
                inBlockComment = true;
                i++;
                continue;
            }

            sb.append(current);
        }

        if (inBlockComment) {
            throw new JsonParseException("Unterminated block comment.");
        }
        return sb.toString();
    }
}
