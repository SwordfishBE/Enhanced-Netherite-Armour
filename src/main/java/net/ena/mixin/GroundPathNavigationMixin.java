package net.ena.mixin;

import net.ena.util.HorseLavaProtection;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
public abstract class GroundPathNavigationMixin {

    @Inject(method = "hasValidPathType", at = @At("HEAD"), cancellable = true)
    private void ena$allowProtectedHorseLavaPathTypes(PathType pathType, CallbackInfoReturnable<Boolean> cir) {
        if (!HorseLavaProtection.isProtectedLavaHorse(((PathNavigationAccessor) this).ena$getMob())) {
            return;
        }

        if (pathType == PathType.LAVA
                || pathType == PathType.FIRE_IN_NEIGHBOR
                || pathType == PathType.WATER
                || pathType == PathType.WATER_BORDER) {
            cir.setReturnValue(true);
        }
    }
}
