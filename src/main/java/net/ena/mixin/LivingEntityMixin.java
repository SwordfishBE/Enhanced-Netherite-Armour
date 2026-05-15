package net.ena.mixin;

import net.ena.util.HorseLavaProtection;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Redirect(
            method = "travelInFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"
            )
    )
    private boolean ena$useWaterTravelForProtectedLavaHorse(LivingEntity entity) {
        return entity.isInWater()
                || entity.isInLava() && HorseLavaProtection.isProtectedLavaHorse(entity);
    }
}
