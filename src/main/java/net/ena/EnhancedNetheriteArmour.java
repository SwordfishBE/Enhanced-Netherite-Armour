package net.ena;

import net.ena.command.EnaCommand;
import net.ena.config.EnaConfig;
import net.ena.effect.ArmorEffectService;
import net.ena.permission.PermissionManager;
import net.ena.player.PlayerToggleManager;
import net.ena.util.ModrinthUpdateChecker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnhancedNetheriteArmour implements ModInitializer {

    public static final String MOD_ID = "enhancednetheritearmour";
    public static final String MOD_NAME = "Enhanced Netherite Armour";
    public static final String LOG_NAME = "EnhancedNetheriteArmour";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModMetadata MOD_METADATA = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow(() -> new IllegalStateException("Missing mod container for " + MOD_ID))
            .getMetadata();
    public static final String MOD_VERSION = MOD_METADATA.getVersion().getFriendlyString();

    private static final ArmorEffectService ARMOR_EFFECT_SERVICE = new ArmorEffectService();
    private static final PlayerToggleManager PLAYER_TOGGLE_MANAGER = new PlayerToggleManager();

    @Override
    public void onInitialize() {
        EnaConfig.load();
        PermissionManager.refreshState(EnaConfig.get());
        PLAYER_TOGGLE_MANAGER.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EnaCommand.register(dispatcher)
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> ARMOR_EFFECT_SERVICE.tick(server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ModrinthUpdateChecker.checkOnceAsync());

        LOGGER.info("{} Mod initialized. Version: {}", prefix(), MOD_VERSION);
    }

    public static PlayerToggleManager getPlayerToggleManager() {
        return PLAYER_TOGGLE_MANAGER;
    }

    public static ArmorEffectService getArmorEffectService() {
        return ARMOR_EFFECT_SERVICE;
    }

    public static EnaConfig loadConfigForEditing() {
        return EnaConfig.loadForEditing();
    }

    public static void applyEditedConfig(EnaConfig editedConfig) {
        EnaConfig.applyEditedConfig(editedConfig);
        PermissionManager.refreshState(EnaConfig.get());
    }

    public static void reloadConfig() {
        EnaConfig.load();
        PermissionManager.refreshState(EnaConfig.get());
    }

    public static String prefix() {
        return "[" + LOG_NAME + "]";
    }
}
