package net.ena.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EnhancedNetheriteArmourClothConfigScreen {

    private EnhancedNetheriteArmourClothConfigScreen() {
    }

    public static Screen create(Screen parent) {
        EnaConfig config = EnhancedNetheriteArmour.loadConfigForEditing();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Enhanced Netherite Armour Config"))
                .setSavingRunnable(() -> EnhancedNetheriteArmour.applyEditedConfig(config));

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        general.addEntry(entries.startBooleanToggle(Component.literal("Enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Master switch for the mod."))
                .setSaveConsumer(value -> config.enabled = value)
                .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Use LuckPerms"), config.useLuckPerms)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Use LuckPerms nodes when the luckperms mod is installed."))
                .setSaveConsumer(value -> config.useLuckPerms = value)
                .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Allow Player Toggle"), config.allowPlayerToggle)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Allow players to use /ena enable and /ena disable for themselves."))
                .setSaveConsumer(value -> config.allowPlayerToggle = value)
                .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Armored Elytra Support"), config.armoredElytraSupport)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Treat a Netherite armored elytra as the chest piece when supported data is detected."))
                .setSaveConsumer(value -> config.armoredElytraSupport = value)
                .build());

        return builder.build();
    }
}
