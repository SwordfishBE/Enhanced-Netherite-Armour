package net.ena.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionManager {

    public static final String USE_PERMISSION = "enhancednetheritearmour.use";
    public static final String TOGGLE_PERMISSION = "enhancednetheritearmour.toggle";

    private static boolean luckPermsInstalled;
    private static boolean luckPermsActive;

    private PermissionManager() {
    }

    public static void refreshState(EnaConfig config) {
        luckPermsInstalled = FabricLoader.getInstance().isModLoaded("luckperms");
        luckPermsActive = config.useLuckPerms && luckPermsInstalled;

        if (luckPermsActive) {
            EnhancedNetheriteArmour.LOGGER.debug("{} LuckPerms permissions are active.",
                    EnhancedNetheriteArmour.prefix());
            return;
        }

        if (config.useLuckPerms) {
            EnhancedNetheriteArmour.LOGGER.warn("{} useLuckPerms is enabled, but LuckPerms is not installed. Falling back to open access.",
                    EnhancedNetheriteArmour.prefix());
            return;
        }

        EnhancedNetheriteArmour.LOGGER.debug("{} LuckPerms integration is disabled in the config.",
                EnhancedNetheriteArmour.prefix());
    }

    public static boolean isLuckPermsActive() {
        return luckPermsActive;
    }

    public static boolean canUse(ServerPlayer player) {
        return !luckPermsActive || Permissions.check(player, USE_PERMISSION, false);
    }

    public static boolean canToggle(ServerPlayer player) {
        return !luckPermsActive || Permissions.check(player, TOGGLE_PERMISSION, false);
    }
}
