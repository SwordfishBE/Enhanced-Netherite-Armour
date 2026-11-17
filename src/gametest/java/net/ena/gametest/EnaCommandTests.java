package net.ena.gametest;

import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.ena.permission.PermissionManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionEvents;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class EnaCommandTests {
    @GameTest
    public void commandsKeepExistingAccessRules(GameTestHelper helper) throws ReflectiveOperationException {
        var player = helper.makeMockServerPlayerInLevel();
        var server = helper.getLevel().getServer();
        var players = server.getPlayerList();
        var id = player.getUUID();
        var toggles = EnhancedNetheriteArmour.getPlayerToggleManager();
        var mode = PermissionManager.class.getDeclaredField("luckPermsActive");
        mode.setAccessible(true);
        var config = EnaConfig.get();
        boolean originalToggle = config.allowPlayerToggle;
        var grant = new AtomicReference<Boolean>();
        var seenNode = new AtomicReference<String>();
        PermissionEvents.ON_REQUEST.register(new PermissionEvents.OnRequest() {
            @Override
            public <T> T handlePermissionRequest(PermissionContext context, PermissionNode<T> node) {
                if (!id.equals(context.uuid()) || !node.key().getNamespace().equals("enhancednetheritearmour")) {
                    return null;
                }
                seenNode.set(node.key().toString());
                Boolean decision = grant.get();
                return decision == null ? null : node.cast(decision);
            }
        });
        try {
            var source = player.createCommandSourceStack();
            server.getCommands().sendCommands(player);
            helper.assertTrue(run(source, "ena") == 1, "Status must remain public");
            helper.assertTrue(run(source, "ena reload") == 0 && EnaConfig.get() == config,
                    "Non-OP must not reload or change active config");
            helper.assertTrue(run(server.createCommandSourceStack(), "ena disable") == 0, "Toggle requires a player");

            config.allowPlayerToggle = false;
            helper.assertTrue(run(source, "ena disable") == 0 && toggles.isEnabled(id), "Disabled config must reject toggle");
            helper.assertTrue(run(source, "ena enable") == 0, "Disabled config must also reject enable");
            config.allowPlayerToggle = true;
            helper.assertTrue(run(source, "ena disable") == 1 && !toggles.isEnabled(id), "Open access must allow disable");
            helper.assertTrue(run(source, "ena enable") == 1 && toggles.isEnabled(id), "Open access must allow enable");

            // Exercise the enabled LuckPerms branch with a simulated Fabric v1 provider.
            mode.setBoolean(null, true);
            helper.assertTrue(!PermissionManager.canUse(player), "Undefined use node must deny");
            helper.assertTrue("enhancednetheritearmour:use".equals(seenNode.get()), "Use node changed");
            helper.assertTrue(run(source, "ena disable") == 0 && toggles.isEnabled(id), "Undefined toggle node must deny");
            helper.assertTrue("enhancednetheritearmour:toggle".equals(seenNode.get()), "Toggle node changed");
            grant.set(true);
            helper.assertTrue(PermissionManager.canUse(player), "Explicit use grant must allow");
            helper.assertTrue(run(source, "ena disable") == 1 && !toggles.isEnabled(id), "Explicit toggle grant must allow");
            grant.set(false);
            helper.assertTrue(!PermissionManager.canUse(player), "Explicit use denial must deny");
            helper.assertTrue(run(source, "ena enable") == 0 && !toggles.isEnabled(id), "Explicit toggle denial must preserve preference");
            grant.set(true);
            helper.assertTrue(run(source, "ena enable") == 1 && toggles.isEnabled(id), "Explicit grant must allow enable");
            helper.assertTrue(run(source, "ena reload") == 0, "A LuckPerms toggle grant must not grant reload");

            players.op(player.nameAndId(), Optional.of(LevelBasedPermissionSet.MODERATOR), Optional.empty());
            helper.assertTrue(run(player.createCommandSourceStack(), "ena reload") == 0, "Moderator must not reload");
            players.op(player.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
            server.getCommands().sendCommands(player);
            helper.assertTrue(run(player.createCommandSourceStack(), "ena reload") == 1 && EnaConfig.get() != config,
                    "Gamemaster must reload successfully");
        } finally {
            config.allowPlayerToggle = originalToggle;
            PermissionManager.refreshState(EnaConfig.get());
            players.deop(player.nameAndId());
            toggles.setEnabled(id, true);
            grant.set(null);
        }
        helper.succeed();
    }

    private static int run(CommandSourceStack source, String command) {
        int[] result = {0};
        source.getServer().getCommands().performPrefixedCommand(source.withCallback((success, value) -> {
            if (success) {
                result[0] = value;
            }
        }), command);
        return result[0];
    }
}
