package net.ena.gametest;

import net.ena.config.EnaConfig;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public final class HorseLavaTests {
    private static void pool(GameTestHelper helper, boolean lava) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 1; y <= 9; y++) {
                    boolean wall = x == 0 || x == 15 || z == 0 || z == 15 || y == 1;
                    helper.setBlock(x, y, z, wall ? Blocks.STONE : y < 9
                            ? (lava ? Blocks.LAVA : Blocks.WATER) : Blocks.AIR);
                }
            }
        }
    }

    private static Horse horse(GameTestHelper helper, Vec3 position, boolean protectedHorse, boolean tempt) {
        Horse horse = helper.spawn(EntityTypes.HORSE, position);
        horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.225);
        horse.getRandom().setSeed(12345L);
        if (!tempt) {
            horse.getGoalSelector().removeAllGoals(goal -> !(goal instanceof FloatGoal));
        }
        if (protectedHorse) {
            horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.NETHERITE_HORSE_ARMOR));
        }
        return horse;
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 180)
    public void submergedHorseReturnsToSurface(GameTestHelper helper) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(8, 3, 8), true, false);
        double start = horse.getY();
        float health = horse.getHealth();
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(horse.getY() > start + 4, "Submerged horse did not return to the surface");
            helper.assertTrue(horse.getHealth() == health, "Protected horse took lava damage");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 180)
    public void fastFallingHorseRecovers(GameTestHelper helper) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(8, 13, 8), true, false);
        horse.setDeltaMovement(0, -3, 0);
        double surface = helper.absoluteVec(new Vec3(8, 8, 8)).y;
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(horse.getY() >= surface, "Fast falling horse remained at the bottom");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 420)
    public void navigationMakesStraightProgressInLava(GameTestHelper helper) {
        navigation(helper, true);
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 420)
    public void navigationWaterControl(GameTestHelper helper) {
        navigation(helper, false);
    }

    private static void navigation(GameTestHelper helper, boolean lava) {
        pool(helper, lava);
        Horse horse = horse(helper, new Vec3(3, 8, 8), true, false);
        Vec3 start = horse.position();
        Vec3 destination = helper.absoluteVec(new Vec3(12, 9, 8));
        helper.onEachTick(() -> {
            if (helper.getTick() >= 10 && helper.getTick() % 10 == 0 && horse.getNavigation().isDone()) {
                horse.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0);
            }
        });
        helper.runAtTickTime(390, () -> {
            helper.assertTrue(horse.getX() > start.x + 5, "Horse did not navigate forward: " + horse.position()
                    + "; start=" + start + "; target=" + destination + "; path=" + horse.getNavigation().getPath());
            helper.assertTrue(Math.abs(horse.getZ() - start.z) < 2, "Horse drifted away from its straight route");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 20)
    public void submergedNavigationUsesLavaSurface(GameTestHelper helper) {
        surfaceReference(helper, true);
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 20)
    public void submergedNavigationWaterControl(GameTestHelper helper) {
        surfaceReference(helper, false);
    }

    private static void surfaceReference(GameTestHelper helper, boolean lava) {
        pool(helper, lava);
        Horse horse = horse(helper, new Vec3(8, 3, 8), true, false);
        int surface = (int) helper.absoluteVec(new Vec3(8, 9, 8)).y;
        helper.runAtTickTime(3, () -> {
            try {
                var method = GroundPathNavigation.class.getDeclaredMethod("getSurfaceY");
                method.setAccessible(true);
                int actual = (Integer) method.invoke(horse.getNavigation());
                helper.assertTrue(actual == surface,
                        "Navigation uses " + actual + " instead of liquid surface " + surface);
                var evaluator = horse.getNavigation().getNodeEvaluator();
                BlockPos position = horse.blockPosition();
                evaluator.prepare(new PathNavigationRegion(helper.getLevel(),
                        position.offset(-3, -3, -3), position.offset(3, 3, 3)), horse);
                try {
                    var floor = WalkNodeEvaluator.class.getDeclaredMethod("getFloorLevel", BlockPos.class);
                    floor.setAccessible(true);
                    double height = (Double) floor.invoke(evaluator, position);
                    helper.assertTrue(height == position.getY() + 0.5,
                            "Swimmable path node uses the wrong floor height: " + height);
                } finally {
                    evaluator.done();
                }
                helper.succeed();
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 220)
    public void goldenCarrotAttractsHorseInLava(GameTestHelper helper) {
        carrot(helper, false);
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 220)
    public void submergedHorseApproachesGoldenCarrot(GameTestHelper helper) {
        carrot(helper, true);
    }

    private static void carrot(GameTestHelper helper, boolean submerged) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(3, submerged ? 4 : 8, 8), true, true);
        var player = helper.makeMockServerPlayerInLevel();
        Vec3 destination = helper.absoluteVec(new Vec3(11, 9, 8));
        player.snapTo(destination.x, destination.y, destination.z, 0, 0);
        player.setNoGravity(true);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 500));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_CARROT));
        double start = horse.getX();
        helper.runAtTickTime(190, () -> {
            helper.assertTrue(horse.getX() > start + 4, "Horse did not approach the golden carrot");
            helper.assertTrue(Math.abs(horse.getZ() - destination.z) < 2, "Tempted horse wandered off its route");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 20)
    public void surfaceFixRespectsConfigAndArmor(GameTestHelper helper) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(8, 3, 8), true, false);
        int surface = (int) helper.absoluteVec(new Vec3(8, 9, 8)).y;
        helper.runAtTickTime(3, () -> {
            EnaConfig config = EnaConfig.get();
            boolean original = config.horseFireProtection;
            try {
                var method = GroundPathNavigation.class.getDeclaredMethod("getSurfaceY");
                method.setAccessible(true);
                config.horseFireProtection = false;
                helper.assertTrue((Integer) method.invoke(horse.getNavigation()) < surface,
                        "Disabled horse protection still changes lava navigation");
                config.horseFireProtection = original;
                horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
                helper.assertTrue((Integer) method.invoke(horse.getNavigation()) < surface,
                        "Unarmored horse still uses protected lava navigation");
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            } finally {
                config.horseFireProtection = original;
            }
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 20)
    public void shallowFlowingLavaStillProtectsHorse(GameTestHelper helper) {
        pool(helper, true);
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                for (int y = 2; y < 9; y++) {
                    helper.setBlock(x, y, z, y == 2
                            ? Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 4)
                            : Blocks.AIR.defaultBlockState());
                }
            }
        }
        Horse horse = horse(helper, new Vec3(8, 2.1, 8), true, false);
        float health = horse.getHealth();
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(horse.hasEffect(MobEffects.FIRE_RESISTANCE), "No protection in flowing lava");
            helper.assertTrue(horse.getHealth() == health, "Horse took flowing lava damage");
            helper.assertTrue(horse.getY() > helper.absoluteVec(new Vec3(8, 2, 8)).y,
                    "Horse sank through shallow lava");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 50)
    public void armorRemovalRestoresPathingAndEffect(GameTestHelper helper) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(8, 8, 8), true, false);
        float original = horse.getPathfindingMalus(PathType.LAVA);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(horse.hasEffect(MobEffects.FIRE_RESISTANCE), "Protection was not applied");
            horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        });
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(!horse.hasEffect(MobEffects.FIRE_RESISTANCE), "Owned effect remained after armor removal");
            helper.assertTrue(horse.getPathfindingMalus(PathType.LAVA) == original, "Original lava path cost was not restored");
            helper.succeed();
        });
    }

    @GameTest(structure = "ena_lava_tests:pool", maxTicks = 50)
    public void submergedRiderDismounts(GameTestHelper helper) {
        pool(helper, true);
        Horse horse = horse(helper, new Vec3(8, 3, 8), true, false);
        var passenger = helper.makeMockServerPlayerInLevel();
        passenger.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100));
        passenger.snapTo(horse.getX(), horse.getY(), horse.getZ(), 0, 0);
        passenger.startRiding(horse, true, false);
        // The mock has no real client connection to drive the player's normal simulation tick.
        helper.onEachTick(() -> {
            if (passenger.isPassenger()) {
                passenger.doTick();
            }
        });
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(!passenger.isPassenger(), "Submerged passenger was not dismounted; eyeY="
                    + passenger.getEyeY() + "; horseY=" + horse.getY());
            helper.succeed();
        });
    }
}
