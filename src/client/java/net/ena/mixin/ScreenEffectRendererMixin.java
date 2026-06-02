package net.ena.mixin;

import net.ena.util.FireVisualProtection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"
            )
    )
    private boolean ena$hideProtectedFirstPersonFireOverlay(LocalPlayer player) {
        return player.isOnFire() && !FireVisualProtection.shouldHideFireAnimation(player);
    }
}
