package net.ena.util;

import net.ena.EnhancedNetheriteArmour;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Optional;

public final class ArmoredElytraSupport {

    private static final String CHESTPLATE_DATA_KEY = "armored_elytra:chestplate";
    private static final String DATAPACK_ROOT_KEY = "armored_elytra";
    private static final float DATAPACK_NETHERITE_MODEL_FLOAT = 13522556.0F;
    private static final double EXPECTED_NETHERITE_ARMOR = 8.0D;
    private static final double EXPECTED_NETHERITE_TOUGHNESS = 3.0D;
    private static final double EXPECTED_NETHERITE_KNOCKBACK_RESISTANCE = 0.1D;
    private static final double EPSILON = 0.0001D;

    private ArmoredElytraSupport() {
    }

    private static final String ELYTRA_DATA_KEY = "armored_elytra:elytra";
    private static final String ITEM_ID_KEY = "id";
    private static final String NETHERITE_CHESTPLATE_ID = "minecraft:netherite_chestplate";
    private static final String NETHERITE_CHESTPLATE_MODEL_STRING = "minecraft:netherite_chestplate";

    public static boolean isNetheriteArmoredElytra(ItemStack chestItem) {
        if (!chestItem.is(Items.ELYTRA)) {
            return false;
        }

        CompoundTag customData = chestItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (isModNetheriteArmoredElytra(chestItem, customData)) {
            return true;
        }

        return isDatapackNetheriteArmoredElytra(chestItem, customData);
    }

    private static boolean isModNetheriteArmoredElytra(ItemStack chestItem, CompoundTag customData) {
        Optional<CompoundTag> chestplateData = customData.getCompound(CHESTPLATE_DATA_KEY);
        Optional<CompoundTag> elytraData = customData.getCompound(ELYTRA_DATA_KEY);
        if (chestplateData.isEmpty() || chestplateData.get().isEmpty() || elytraData.isEmpty() || elytraData.get().isEmpty()) {
            return false;
        }

        String embeddedChestplateId = chestplateData.get().getStringOr(ITEM_ID_KEY, "");
        if (NETHERITE_CHESTPLATE_ID.equals(embeddedChestplateId)) {
            return true;
        }

        CustomModelData customModelData = chestItem.get(DataComponents.CUSTOM_MODEL_DATA);
        String modelString = customModelData == null ? null : customModelData.getString(0);
        if (NETHERITE_CHESTPLATE_MODEL_STRING.equals(modelString)) {
            EnhancedNetheriteArmour.LOGGER.debug("{} Armored Elytra fallback matched via custom model data string.",
                    EnhancedNetheriteArmour.prefix());
            return true;
        }

        return false;
    }

    private static boolean isDatapackNetheriteArmoredElytra(ItemStack chestItem, CompoundTag customData) {
        Optional<CompoundTag> armoredElytraData = customData.getCompound(DATAPACK_ROOT_KEY);
        if (armoredElytraData.isEmpty() || !armoredElytraData.get().getBooleanOr("armored", false)) {
            return false;
        }

        CustomModelData customModelData = chestItem.get(DataComponents.CUSTOM_MODEL_DATA);
        Float modelFloat = customModelData == null ? null : customModelData.getFloat(0);
        if (modelFloat == null || Math.abs(modelFloat - DATAPACK_NETHERITE_MODEL_FLOAT) > EPSILON) {
            return false;
        }

        ItemAttributeModifiers attributes = chestItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributes == null) {
            return false;
        }

        double armor = attributes.compute(Attributes.ARMOR, 0.0D, EquipmentSlot.CHEST);
        double toughness = attributes.compute(Attributes.ARMOR_TOUGHNESS, 0.0D, EquipmentSlot.CHEST);
        double knockbackResistance = attributes.compute(Attributes.KNOCKBACK_RESISTANCE, 0.0D, EquipmentSlot.CHEST);
        return nearlyEquals(armor, EXPECTED_NETHERITE_ARMOR)
                && nearlyEquals(toughness, EXPECTED_NETHERITE_TOUGHNESS)
                && nearlyEquals(knockbackResistance, EXPECTED_NETHERITE_KNOCKBACK_RESISTANCE);
    }

    private static boolean nearlyEquals(double actual, double expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }
}
