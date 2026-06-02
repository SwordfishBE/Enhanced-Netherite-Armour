package net.ena.mixin;

import net.ena.util.FireVisualProtection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "displayFireAnimation", at = @At("HEAD"), cancellable = true)
    private void ena$hideProtectedFireAnimation(CallbackInfoReturnable<Boolean> cir) {
        if (FireVisualProtection.shouldHideFireAnimation((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
