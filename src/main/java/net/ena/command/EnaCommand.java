package net.ena.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.ena.permission.PermissionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class EnaCommand {

    private EnaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ena")
                        .executes(EnaCommand::executeInfo)
                        .then(Commands.literal("enable").executes(EnaCommand::executeEnable))
                        .then(Commands.literal("disable").executes(EnaCommand::executeDisable))
                        .then(Commands.literal("reload")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(EnaCommand::executeReload))
        );
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendSuccess(() -> Component.literal(
                    EnhancedNetheriteArmour.prefix() + " /ena enable, /ena disable, /ena reload"
            ), false);
            return 1;
        }

        EnaConfig config = EnaConfig.get();
        boolean armorActive = EnhancedNetheriteArmour.getArmorEffectService().hasQualifiedArmorCombination(player, config);
        boolean playerEnabled = !config.allowPlayerToggle
                || EnhancedNetheriteArmour.getPlayerToggleManager().isEnabled(player.getUUID());
        String permissionMode = PermissionManager.isLuckPermsActive() ? "LuckPerms" : "Open access";

        context.getSource().sendSuccess(() -> Component.literal(
                "§6" + EnhancedNetheriteArmour.prefix() + "§r Armor check: " + (armorActive ? "§aACTIVE" : "§cINACTIVE")
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "§6" + EnhancedNetheriteArmour.prefix() + "§r Player toggle: "
                        + (config.allowPlayerToggle ? (playerEnabled ? "§aENABLED" : "§cDISABLED") : "§7Not configurable")
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
                "§6" + EnhancedNetheriteArmour.prefix() + "§r Permission mode: §e" + permissionMode
        ), false);
        return 1;
    }

    private static int executeEnable(CommandContext<CommandSourceStack> context) {
        return executeToggle(context, true);
    }

    private static int executeDisable(CommandContext<CommandSourceStack> context) {
        return executeToggle(context, false);
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context, boolean enabled) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal(
                    EnhancedNetheriteArmour.prefix() + " This command can only be used by a player."
            ));
            return 0;
        }

        EnaConfig config = EnaConfig.get();
        if (!config.allowPlayerToggle) {
            context.getSource().sendFailure(Component.literal(
                    EnhancedNetheriteArmour.prefix() + " Player toggles are disabled in the config."
            ));
            return 0;
        }

        if (!PermissionManager.canToggle(player)) {
            context.getSource().sendFailure(Component.literal(
                    EnhancedNetheriteArmour.prefix() + " You do not have permission to use this command."
            ));
            return 0;
        }

        EnhancedNetheriteArmour.getPlayerToggleManager().setEnabled(player.getUUID(), enabled);
        EnhancedNetheriteArmour.getArmorEffectService().refreshPlayer(player);

        context.getSource().sendSuccess(() -> Component.literal(
                "§6" + EnhancedNetheriteArmour.prefix() + "§r Fire Resistance support "
                        + (enabled ? "§aenabled" : "§cdisabled") + "§r."
        ), false);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        EnhancedNetheriteArmour.reloadConfig();
        context.getSource().sendSuccess(() -> Component.literal(
                "§6" + EnhancedNetheriteArmour.prefix() + "§r Config reloaded."
        ), true);
        EnhancedNetheriteArmour.LOGGER.debug("{} Config reloaded via command.", EnhancedNetheriteArmour.prefix());
        return 1;
    }
}
