package net.ena.mixin;

import net.ena.util.AnimalLavaProtection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    @WrapOperation(method = "getStart", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isInFloatableFluid()Z"))
    private boolean ena$startAtLavaSurface(Mob entity, Operation<Boolean> original) {
        return original.call(entity)
                || entity.isInLava() && AnimalLavaProtection.isProtected(entity);
    }

    @WrapOperation(method = {"getStart", "getFloorLevel(Lnet/minecraft/core/BlockPos;)D"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean ena$useLavaSurfaceForNodes(FluidState state, TagKey<Fluid> tag, Operation<Boolean> original) {
        return original.call(state, tag)
                || tag.equals(FluidTags.ENTITY_FLOATABLE) && state.is(FluidTags.LAVA)
                && AnimalLavaProtection.isProtected(mob);
    }

    @Inject(method = "getPathTypeOfMob", at = @At("RETURN"), cancellable = true)
    private void ena$treatProtectedHorseLavaLikeWater(
            PathfindingContext context,
            int x,
            int y,
            int z,
            Mob mob,
            CallbackInfoReturnable<PathType> cir) {
        if (!AnimalLavaProtection.isProtected(mob)) {
            return;
        }

        PathType pathType = cir.getReturnValue();
        if (pathType == PathType.LAVA) {
            cir.setReturnValue(PathType.WATER);
        } else if (pathType == PathType.FIRE_IN_NEIGHBOR) {
            cir.setReturnValue(PathType.WATER_BORDER);
        }
    }
}
