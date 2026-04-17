package net.ena.effect;

import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.ena.permission.PermissionManager;
import net.ena.util.ArmoredElytraSupport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ArmorEffectService {

    private static final int EFFECT_DURATION_TICKS = 340;
    private static final int REFRESH_THRESHOLD_TICKS = 80;
    private final Set<UUID> managedPlayers = new HashSet<>();

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayer(player);
        }
    }

    public void refreshPlayer(ServerPlayer player) {
        EnaConfig config = EnaConfig.get();
        if (!config.enabled) {
            removeManagedEffect(player);
            return;
        }

        boolean shouldApply = PermissionManager.canUse(player)
                && isPlayerToggleEnabled(player, config)
                && hasQualifiedArmorCombination(player, config);

        if (shouldApply) {
            ensureEffect(player);
        } else {
            removeManagedEffect(player);
        }
    }

    public boolean hasQualifiedArmorCombination(ServerPlayer player, EnaConfig config) {
        boolean hasHelmet = player.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET);
        boolean hasLeggings = player.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS);
        boolean hasBoots = player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
        if (!hasHelmet || !hasLeggings || !hasBoots) {
            return false;
        }

        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestItem.is(Items.NETHERITE_CHESTPLATE)) {
            return true;
        }

        return config.armoredElytraSupport && ArmoredElytraSupport.isNetheriteArmoredElytra(chestItem);
    }

    private boolean isPlayerToggleEnabled(ServerPlayer player, EnaConfig config) {
        return !config.allowPlayerToggle || EnhancedNetheriteArmour.getPlayerToggleManager().isEnabled(player.getUUID());
    }

    private void ensureEffect(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current != null) {
            if (isManagedEffect(current)) {
                managedPlayers.add(player.getUUID());
                if (!current.endsWithin(REFRESH_THRESHOLD_TICKS)) {
                    return;
                }
            } else {
                managedPlayers.remove(player.getUUID());
            }
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));

        MobEffectInstance updated = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (updated != null && isManagedEffect(updated)) {
            managedPlayers.add(player.getUUID());
        }
    }

    private void removeManagedEffect(ServerPlayer player) {
        if (!managedPlayers.contains(player.getUUID())) {
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current == null) {
            managedPlayers.remove(player.getUUID());
            return;
        }

        if (!isManagedEffect(current)) {
            managedPlayers.remove(player.getUUID());
            return;
        }

        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        managedPlayers.remove(player.getUUID());
    }

    private boolean isManagedEffect(MobEffectInstance effect) {
        return effect.getAmplifier() == 0
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()
                && effect.endsWithin(EFFECT_DURATION_TICKS);
    }
}
