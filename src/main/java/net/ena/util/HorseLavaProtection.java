package net.ena.util;

import net.ena.config.EnaConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Items;

public final class HorseLavaProtection {

    private HorseLavaProtection() {
    }

    public static boolean isProtectedLavaHorse(Entity entity) {
        return entity instanceof Horse horse && isProtectedLavaHorse(horse);
    }

    public static boolean isProtectedLavaHorse(Horse horse) {
        EnaConfig config = EnaConfig.get();
        return config.enabled
                && config.horseFireProtection
                && horse.getItemBySlot(EquipmentSlot.BODY).is(Items.NETHERITE_HORSE_ARMOR);
    }
}
