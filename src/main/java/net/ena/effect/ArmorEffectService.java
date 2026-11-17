package net.ena.effect;

import net.ena.EnhancedNetheriteArmour;
import net.ena.config.EnaConfig;
import net.ena.permission.PermissionManager;
import net.ena.util.ArmoredElytraSupport;
import net.ena.util.AnimalLavaProtection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArmorEffectService {

    private static final int REFRESH_THRESHOLD_TICKS = 80;
    private static final double LAVA_FLOAT_MIN_UPWARD_SPEED = 0.06D;
    private static final double LAVA_FLOAT_MAX_UPWARD_SPEED = 0.16D;
    private static final double LAVA_FLOAT_RECOVERY_UPWARD_SPEED = 0.28D;
    private static final double LAVA_SURFACE_FLUID_HEIGHT = 0.55D;
    private static final double LAVA_DEEP_FLUID_HEIGHT = 1.2D;
    private static final double LAVA_FAST_SINKING_SPEED = -0.08D;
    private final Map<UUID, AnimalPathingMalus> managedAnimalPathing = new HashMap<>();

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayer(player);
        }
        refreshAnimals(server);
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
            clearFireVisuals(player);
            ensureManagedEffect(player);
        } else {
            removeManagedEffect(player);
        }
    }

    private void refreshAnimals(MinecraftServer server) {
        Set<UUID> protectedIds = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Horse horse : level.getEntities(EntityTypes.HORSE, horse -> true)) {
                refreshAnimal(horse, protectedIds);
            }
            for (Wolf wolf : level.getEntities(EntityTypes.WOLF, wolf -> true)) {
                refreshAnimal(wolf, protectedIds);
            }
        }

        managedAnimalPathing.keySet().retainAll(protectedIds);
    }

    private void refreshAnimal(Mob animal, Set<UUID> protectedIds) {
        if (!AnimalLavaProtection.isProtected(animal)) {
            restorePathing(animal);
            removeManagedEffect(animal);
            return;
        }
        protectedIds.add(animal.getUUID());
        enableLavaPathing(animal);
        clearFireVisuals(animal);
        floatInLava(animal);
        if (animal instanceof Horse horse) {
            dismountPassengersUnderLava(horse);
        }
        ensureManagedEffect(animal);
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

    private void clearFireVisuals(LivingEntity entity) {
        if (entity.getRemainingFireTicks() > 0 || entity.isOnFire()) {
            entity.clearFire();
            entity.setRemainingFireTicks(0);
        }
    }

    private void floatInLava(Mob animal) {
        if (!animal.isInLava()) {
            return;
        }

        Vec3 movement = animal.getDeltaMovement();
        double fluidHeight = animal.getFluidHeight(FluidTags.LAVA);
        double upwardSpeed = lavaUpwardSpeed(fluidHeight, movement.y);

        if (movement.y < upwardSpeed) {
            animal.setDeltaMovement(
                    movement.x,
                    upwardSpeed,
                    movement.z);
        }
        animal.resetFallDistance();
    }

    private double lavaUpwardSpeed(double fluidHeight, double verticalMovement) {
        if (fluidHeight >= LAVA_DEEP_FLUID_HEIGHT || verticalMovement <= LAVA_FAST_SINKING_SPEED) {
            return LAVA_FLOAT_RECOVERY_UPWARD_SPEED;
        }
        return fluidHeight > LAVA_SURFACE_FLUID_HEIGHT
                ? LAVA_FLOAT_MAX_UPWARD_SPEED
                : LAVA_FLOAT_MIN_UPWARD_SPEED;
    }

    private void dismountPassengersUnderLava(Horse horse) {
        if (horse.isVehicle() && horse.hasPassenger(this::isPassengerUnderLava)) {
            horse.ejectPassengers();
        }
    }

    private boolean isPassengerUnderLava(Entity passenger) {
        return passenger.isEyeInFluid(FluidTags.LAVA);
    }

    private void enableLavaPathing(Mob animal) {
        managedAnimalPathing.computeIfAbsent(animal.getUUID(), ignored -> AnimalPathingMalus.capture(animal));
        animal.setPathfindingMalus(PathType.LAVA, PathType.WATER.getMalus());
        animal.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, PathType.WATER_BORDER.getMalus());
        animal.setPathfindingMalus(PathType.WATER, PathType.WATER.getMalus());
        animal.setPathfindingMalus(PathType.WATER_BORDER, PathType.WATER_BORDER.getMalus());
    }

    private void restorePathing(Mob animal) {
        AnimalPathingMalus original = managedAnimalPathing.remove(animal.getUUID());
        if (original != null) {
            original.restore(animal);
            if (animal instanceof Wolf) {
                // A cached route can still cross lava using the old path costs.
                animal.getNavigation().stop();
            }
        }
    }

    private void ensureManagedEffect(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        if (current != null) {
            if (!ManagedFireResistance.isOwned(current) || !current.endsWithin(REFRESH_THRESHOLD_TICKS)) {
                return;
            }
            // Replace only our untouched effect; updating in place would relinquish ownership.
            entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        entity.addEffect(new ManagedFireResistance(ManagedFireResistance.DURATION_TICKS));
    }

    private void removeManagedEffect(LivingEntity entity) {
        if (ManagedFireResistance.isOwned(entity.getEffect(MobEffects.FIRE_RESISTANCE))) {
            entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
    }

    private void clearManagedAnimals(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Horse horse : level.getEntities(EntityTypes.HORSE, horse -> true)) {
                onEntityUnload(horse);
            }
            for (Wolf wolf : level.getEntities(EntityTypes.WOLF, wolf -> true)) {
                onEntityUnload(wolf);
            }
        }
        managedAnimalPathing.clear();
    }

    public void onEntityUnload(Entity entity) {
        if (entity instanceof Mob animal && (animal instanceof Horse || animal instanceof Wolf)) {
            restorePathing(animal);
            removeManagedEffect(animal);
        } else if (entity instanceof ServerPlayer player) {
            removeManagedEffect(player);
        }
    }

    public void stop(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeManagedEffect(player);
        }
        clearManagedAnimals(server);
    }

    private record AnimalPathingMalus(float lava, float fireInNeighbor, float water, float waterBorder) {
        private static AnimalPathingMalus capture(Mob horse) {
            return new AnimalPathingMalus(
                    horse.getPathfindingMalus(PathType.LAVA),
                    horse.getPathfindingMalus(PathType.FIRE_IN_NEIGHBOR),
                    horse.getPathfindingMalus(PathType.WATER),
                    horse.getPathfindingMalus(PathType.WATER_BORDER));
        }

        private void restore(Mob horse) {
            horse.setPathfindingMalus(PathType.LAVA, lava);
            horse.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, fireInNeighbor);
            horse.setPathfindingMalus(PathType.WATER, water);
            horse.setPathfindingMalus(PathType.WATER_BORDER, waterBorder);
        }
    }

}
