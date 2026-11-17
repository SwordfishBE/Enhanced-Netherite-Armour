package net.ena.util;

import net.ena.config.EnaConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Items;

public final class AnimalLavaProtection {

    private AnimalLavaProtection() {
    }

    public static boolean isProtected(Entity entity) {
        EnaConfig config = EnaConfig.get();
        if (!config.enabled) {
            return false;
        }
        if (entity instanceof Horse horse) {
            return config.horseFireProtection
                    && horse.getItemBySlot(EquipmentSlot.BODY).is(Items.NETHERITE_HORSE_ARMOR);
        }
        return entity instanceof Wolf wolf && isProtectedWolf(wolf);
    }

    public static boolean isProtectedWolf(Wolf wolf) {
        EnaConfig config = EnaConfig.get();
        return config.enabled && config.wolfFireProtection && wolf.isTame()
                && wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR);
    }
}
