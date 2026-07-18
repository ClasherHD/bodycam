package dev.ClasherHD.bodycam.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void onRenderHands(float f, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
            net.minecraft.client.player.LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (Minecraft.getInstance().getCameraEntity() != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
