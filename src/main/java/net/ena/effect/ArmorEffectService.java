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

public final class ArmorEffectService {

    private static final int EFFECT_DURATION_TICKS = 340;
    private static final int REFRESH_THRESHOLD_TICKS = 80;

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
        if (current != null && current.getAmplifier() == 0 && !current.endsWithin(REFRESH_THRESHOLD_TICKS)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));
    }

    private void removeManagedEffect(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current == null) {
            return;
        }

        if (current.getAmplifier() != 0) {
            return;
        }
        if (current.isAmbient()) {
            return;
        }
        if (current.isVisible()) {
            return;
        }
        if (!current.endsWithin(EFFECT_DURATION_TICKS)) {
            return;
        }

        player.removeEffect(MobEffects.FIRE_RESISTANCE);
    }
}
