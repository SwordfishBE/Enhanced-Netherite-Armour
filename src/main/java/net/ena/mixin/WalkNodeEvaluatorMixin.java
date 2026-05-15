package net.ena.mixin;

import net.ena.util.HorseLavaProtection;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @Inject(method = "getPathTypeOfMob", at = @At("RETURN"), cancellable = true)
    private void ena$treatProtectedHorseLavaLikeWater(
            PathfindingContext context,
            int x,
            int y,
            int z,
            Mob mob,
            CallbackInfoReturnable<PathType> cir) {
        if (!HorseLavaProtection.isProtectedLavaHorse(mob)) {
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
