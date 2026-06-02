package net.ena.util;

import net.ena.config.EnaConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FireVisualProtection {

    private FireVisualProtection() {
    }

    public static boolean shouldHideFireAnimation(Entity entity) {
        return entity instanceof LivingEntity livingEntity
                && (isProtectedPlayer(livingEntity) || HorseLavaProtection.isProtectedLavaHorse(entity));
    }

    private static boolean isProtectedPlayer(LivingEntity entity) {
        if (!(entity instanceof Player) || !hasManagedFireResistance(entity)) {
            return false;
        }

        EnaConfig config = EnaConfig.get();
        return config.enabled && hasQualifiedArmorCombination(entity, config);
    }

    private static boolean hasManagedFireResistance(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        return effect != null
                && effect.getAmplifier() == 0
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon();
    }

    private static boolean hasQualifiedArmorCombination(LivingEntity entity, EnaConfig config) {
        boolean hasHelmet = entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET);
        boolean hasLeggings = entity.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS);
        boolean hasBoots = entity.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
        if (!hasHelmet || !hasLeggings || !hasBoots) {
            return false;
        }

        ItemStack chestItem = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chestItem.is(Items.NETHERITE_CHESTPLATE)
                || config.armoredElytraSupport && ArmoredElytraSupport.isNetheriteArmoredElytra(chestItem);
    }
}
