package net.ena.mixin;

import net.ena.util.AnimalLavaProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void ena$protectWolfFromFire(ServerLevel level, DamageSource source, float amount,
                                        CallbackInfoReturnable<Boolean> cir) {
        // Stop fire hits before the wolf reacts, including the tick before ENA applies its effect.
        if (source.is(DamageTypeTags.IS_FIRE) && AnimalLavaProtection.isProtectedWolf((Wolf) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
