package net.ena.effect;

import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.ena.permission.PermissionManager;
import net.ena.util.ArmoredElytraSupport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ArmorEffectService {

    private static final int EFFECT_DURATION_TICKS = 340;
    private static final int REFRESH_THRESHOLD_TICKS = 80;
    private final Set<UUID> managedPlayers = new HashSet<>();
    private final Set<UUID> managedHorses = new HashSet<>();

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayer(player);
        }
        refreshHorses(server);
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
            ensureManagedEffect(player, managedPlayers);
        } else {
            removeManagedEffect(player);
        }
    }

    private void refreshHorses(MinecraftServer server) {
        EnaConfig config = EnaConfig.get();
        if (!config.enabled || !config.horseFireProtection) {
            clearManagedHorses(server);
            return;
        }

        Set<UUID> seenHorseIds = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Horse horse : level.getEntities(EntityType.HORSE, horse -> true)) {
                seenHorseIds.add(horse.getUUID());
                if (hasNetheriteHorseArmor(horse)) {
                    clearFireVisuals(horse);
                    ensureManagedEffect(horse, managedHorses);
                } else {
                    removeManagedEffect(horse, managedHorses);
                }
            }
        }

        managedHorses.retainAll(seenHorseIds);
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

    private boolean hasNetheriteHorseArmor(Horse horse) {
        return horse.getItemBySlot(EquipmentSlot.BODY).is(Items.NETHERITE_HORSE_ARMOR);
    }

    private void clearFireVisuals(Horse horse) {
        if (horse.getRemainingFireTicks() > 0 || horse.isOnFire()) {
            horse.clearFire();
            horse.setRemainingFireTicks(0);
        }
    }

    private void ensureManagedEffect(LivingEntity entity, Set<UUID> managedEntities) {
        MobEffectInstance current = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current != null) {
            if (isManagedEffect(current)) {
                managedEntities.add(entity.getUUID());
                if (!current.endsWithin(REFRESH_THRESHOLD_TICKS)) {
                    return;
                }
            } else {
                managedEntities.remove(entity.getUUID());
                return;
            }
        }

        entity.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));

        MobEffectInstance updated = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        if (updated != null && isManagedEffect(updated)) {
            managedEntities.add(entity.getUUID());
        }
    }

    private void removeManagedEffect(ServerPlayer player) {
        removeManagedEffect(player, managedPlayers);
    }

    private void removeManagedEffect(LivingEntity entity, Set<UUID> managedEntities) {
        if (!managedEntities.contains(entity.getUUID())) {
            return;
        }

        MobEffectInstance current = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current == null) {
            managedEntities.remove(entity.getUUID());
            return;
        }

        if (!isManagedEffect(current)) {
            managedEntities.remove(entity.getUUID());
            return;
        }

        entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        managedEntities.remove(entity.getUUID());
    }

    private void clearManagedHorses(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Horse horse : level.getEntities(EntityType.HORSE, horse -> managedHorses.contains(horse.getUUID()))) {
                removeManagedEffect(horse, managedHorses);
            }
        }
        managedHorses.clear();
    }

    private boolean isManagedEffect(MobEffectInstance effect) {
        return effect.getAmplifier() == 0
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon()
                && effect.endsWithin(EFFECT_DURATION_TICKS);
    }
}
