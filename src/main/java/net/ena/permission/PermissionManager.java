package net.ena.permission;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionManager {

    // LuckPerms resolves namespace:path as the existing namespace.path node.
    private static final Identifier USE_PERMISSION = Identifier.fromNamespaceAndPath(EnhancedNetheriteArmour.MOD_ID, "use");
    private static final Identifier TOGGLE_PERMISSION = Identifier.fromNamespaceAndPath(EnhancedNetheriteArmour.MOD_ID, "toggle");

    private static boolean luckPermsInstalled;
    private static volatile boolean luckPermsActive;

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
        return !luckPermsActive || ((PermissionContextOwner) player).checkPermission(USE_PERMISSION, false);
    }

    public static boolean canToggle(ServerPlayer player) {
        return !luckPermsActive || ((PermissionContextOwner) player).checkPermission(TOGGLE_PERMISSION, false);
    }
}
