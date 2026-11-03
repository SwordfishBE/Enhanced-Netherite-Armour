package net.ena.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.ena.util.ClothConfigCompatibility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;

public final class EnhancedNetheriteArmourModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        var cloth = FabricLoader.getInstance().getModContainer("cloth-config");
        if (cloth.isEmpty()) {
            return notice("Install Cloth Config for Minecraft 26.4 to edit the configuration here. "
                    + "You can also edit config/enhancednetheritearmour.json and use /ena reload (OP only).");
        }
        String installed = cloth.get().getMetadata().getVersion().getFriendlyString();
        if (!ClothConfigCompatibility.isCompatible(installed)) {
            return notice("The configuration menu requires Cloth Config for Minecraft 26.4. "
                    + "Installed version: " + installed + ". Please update Cloth Config. Until then, edit "
                    + "config/enhancednetheritearmour.json and use /ena reload (OP only).");
        }

        return EnhancedNetheriteArmourClothConfigScreen::create;
    }

    private static ConfigScreenFactory<?> notice(String message) {
        return parent -> new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                Component.literal("Enhanced Netherite Armour"), Component.literal(message));
    }
}
