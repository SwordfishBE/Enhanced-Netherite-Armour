package net.ena.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ena.EnhancedNetheriteArmour;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnaConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("enhancednetheritearmour.json");

    private static EnaConfig instance = new EnaConfig();

    public boolean enabled = true;
    public boolean useLuckPerms = false;
    public boolean allowPlayerToggle = true;
    public boolean armoredElytraSupport = true;

    public static EnaConfig get() {
        return instance;
    }

    public static void load() {
        instance = loadForEditing();
        save();
        EnhancedNetheriteArmour.LOGGER.debug("{} Config loaded.", EnhancedNetheriteArmour.prefix());
    }

    public static EnaConfig loadForEditing() {
        EnaConfig loadedConfig = new EnaConfig();

        if (Files.exists(CONFIG_PATH)) {
            try {
                String rawConfig = Files.readString(CONFIG_PATH);
                String json = stripJsonComments(rawConfig);
                EnaConfig parsed = GSON.fromJson(json, EnaConfig.class);
                if (parsed != null) {
                    loadedConfig = parsed;
                }
            } catch (IOException exception) {
                EnhancedNetheriteArmour.LOGGER.warn("{} Failed to read config, using defaults.",
                        EnhancedNetheriteArmour.prefix(), exception);
            } catch (RuntimeException exception) {
                EnhancedNetheriteArmour.LOGGER.warn("{} Failed to parse config, using defaults.",
                        EnhancedNetheriteArmour.prefix(), exception);
            }
        }

        loadedConfig.normalize();
        return loadedConfig;
    }

    public static void applyEditedConfig(EnaConfig editedConfig) {
        EnaConfig normalized = editedConfig == null ? new EnaConfig() : editedConfig.copy();
        normalized.normalize();
        instance = normalized;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, toCommentedJson(instance));
            EnhancedNetheriteArmour.LOGGER.debug("{} Config saved to {}.",
                    EnhancedNetheriteArmour.prefix(), CONFIG_PATH);
        } catch (IOException exception) {
            EnhancedNetheriteArmour.LOGGER.warn("{} Failed to save config.",
                    EnhancedNetheriteArmour.prefix(), exception);
        }
    }

    public EnaConfig copy() {
        EnaConfig copy = new EnaConfig();
        copy.enabled = enabled;
        copy.useLuckPerms = useLuckPerms;
        copy.allowPlayerToggle = allowPlayerToggle;
        copy.armoredElytraSupport = armoredElytraSupport;
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
        appendComment(sb, "If false, the player commands stay unavailable and eligible players are always treated as enabled.");
        appendProperty(sb, "allowPlayerToggle", config.allowPlayerToggle, true);

        appendComment(sb, "Enable support for Armored Elytra and the compatible datapack variant.");
        appendComment(sb, "If true, a Netherite armored elytra counts as the chest piece for the full-set check.");
        appendProperty(sb, "armoredElytraSupport", config.armoredElytraSupport, false);
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
                inLineComment = true;
                i++;
                continue;
            }

            if (current == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            sb.append(current);
        }

        return sb.toString();
    }
}
