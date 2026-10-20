package net.ena.mixin;

import com.mojang.serialization.Codec;
import net.ena.effect.ManagedEffectCodec;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
    @Shadow
    @Final
    @Mutable
    public static Codec<MobEffectInstance> CODEC;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void ena$persistEffectOwnership(CallbackInfo ci) {
        CODEC = ManagedEffectCodec.wrap(CODEC);
    }
}
