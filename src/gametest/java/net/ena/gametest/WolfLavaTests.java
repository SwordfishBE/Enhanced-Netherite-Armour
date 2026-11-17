package net.ena.gametest;

import net.ena.config.EnaConfig;
import net.ena.effect.ManagedFireResistance;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class WolfLavaTests {
    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 160)
    public void onlyTamedArmoredWolvesAreProtected(GameTestHelper helper) {
        HorseLavaTests.pool(helper, true);
        var dog = helper.spawn(EntityTypes.WOLF, new Vec3(8, 9, 8));
        var wild = helper.spawn(EntityTypes.WOLF, new Vec3(4, 9, 4));
        var unarmored = helper.spawn(EntityTypes.WOLF, new Vec3(12, 9, 12));
        var armor = new ItemStack(Items.WOLF_ARMOR);
        // Keep the default-off control dry until protection is enabled.
        for (int x = 7; x <= 9; x++) {
            for (int z = 7; z <= 9; z++) {
                helper.setBlock(x, 8, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        dog.setTame(true, true);
        unarmored.setTame(true, true);
        dog.setItemSlot(EquipmentSlot.BODY, armor);
        wild.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        dog.getGoalSelector().removeAllGoals(goal -> !(goal instanceof FloatGoal));
        float originalMalus = dog.getPathfindingMalus(PathType.LAVA);
        var level = helper.getLevel();
        helper.runAtTickTime(5, () -> {
            helper.assertFalse(EnaConfig.get().wolfFireProtection, "Wolf protection must default off");
            helper.assertFalse(dog.hasEffect(MobEffects.FIRE_RESISTANCE), "Default must not grant protection");
            EnaConfig.get().wolfFireProtection = true;
            dog.setOrderedToSit(true);
            helper.assertFalse(dog.hurtServer(level, dog.damageSources().inFire(), 4), "Fire protection must apply immediately");
            helper.assertTrue(dog.isOrderedToSit(), "Fire must not cancel a protected dog's sit order");
            dog.setOrderedToSit(false);
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(ManagedFireResistance.isOwned(dog.getEffect(MobEffects.FIRE_RESISTANCE)), "Tamed armored wolf needs protection");
            helper.assertFalse(wild.hasEffect(MobEffects.FIRE_RESISTANCE), "Wild wolf must remain unprotected");
            helper.assertFalse(unarmored.hasEffect(MobEffects.FIRE_RESISTANCE), "Unarmored dog must remain unprotected");
            helper.assertFalse(dog.hurtServer(level, dog.damageSources().inFire(), 4), "Fire must not damage protected dog");
            Vec3 submerged = helper.absoluteVec(new Vec3(11, 3, 8));
            dog.snapTo(submerged.x, submerged.y, submerged.z, 0, 0);
        });
        helper.runAtTickTime(115, () -> {
            helper.assertTrue(dog.getY() > helper.absoluteVec(new Vec3(8, 8, 8)).y, "Dog must rise from deep lava");
            helper.assertTrue(dog.getHealth() == dog.getMaxHealth(), "Lava must not hurt protected dog");
            helper.assertTrue(armor.getDamageValue() == 0, "Lava and fire must not damage wolf armor");
            var target = dog.blockPosition().east(3);
            var route = new Path(List.of(new Node(target.getX(), target.getY(), target.getZ())), target, true);
            helper.assertTrue(dog.getNavigation().moveTo(route, 1), "Test must install an active route before armor removal");
            dog.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        });
        helper.runAtTickTime(117, () -> {
            helper.assertFalse(dog.hasEffect(MobEffects.FIRE_RESISTANCE), "Armor removal must clear ENA protection");
            helper.assertTrue(dog.getPathfindingMalus(PathType.LAVA) == originalMalus, "Armor removal must restore path cost");
            helper.assertTrue(dog.getNavigation().getPath() == null, "Armor removal must discard the cached route");
            dog.setItemSlot(EquipmentSlot.BODY, armor);
        });
        helper.runAtTickTime(119, () -> {
            EnaConfig.get().wolfFireProtection = false;
            dog.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200));
        });
        helper.runAtTickTime(121, () -> {
            helper.assertTrue(dog.hasEffect(MobEffects.FIRE_RESISTANCE), "Disabling ENA must preserve potion protection");
            helper.assertTrue(dog.getPathfindingMalus(PathType.LAVA) == originalMalus, "Disabling ENA must restore path cost");
            dog.removeEffect(MobEffects.FIRE_RESISTANCE);
            EnaConfig.get().wolfFireProtection = true;
        });
        helper.runAtTickTime(124, () -> {
            helper.assertTrue(ManagedFireResistance.isOwned(dog.getEffect(MobEffects.FIRE_RESISTANCE)), "Test must restore protection before unloading");
            dog.discard();
            helper.assertFalse(dog.hasEffect(MobEffects.FIRE_RESISTANCE), "Entity unload must clear the owned effect");
            helper.assertTrue(dog.getPathfindingMalus(PathType.LAVA) == originalMalus, "Entity unload must restore path cost");
            EnaConfig.get().wolfFireProtection = false;
            helper.succeed();
        });
    }
}
