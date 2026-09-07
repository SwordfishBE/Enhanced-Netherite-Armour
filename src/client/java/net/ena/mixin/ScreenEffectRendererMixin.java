package net.ena.mixin;

import net.ena.util.FireVisualProtection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "submit", at = @At("HEAD"))
    private void ena$hideProtectedFirstPersonFireOverlay(
            float tickProgress,
            SubmitNodeCollector submitNodeCollector,
            PlayerRenderState playerRenderState,
            CameraRenderState cameraRenderState,
            boolean renderItemActivationAnimation,
            CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && FireVisualProtection.shouldHideFireAnimation(player)) {
            playerRenderState.isOnFire = false;
        }
    }
}
