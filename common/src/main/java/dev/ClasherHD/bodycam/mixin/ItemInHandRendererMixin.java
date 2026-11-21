package dev.ClasherHD.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.render.CherryHouseMapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void bodycam$hideHandsWhileSpectating(
            float partialTicks,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LocalPlayer player,
            int packedLight,
            CallbackInfo ci
    ) {
        if (BodycamViewScreen.isMonitoring && Minecraft.getInstance().getCameraEntity() != Minecraft.getInstance().player) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMap", at = @At("TAIL"))
    private void bodycam$renderHandMapOverlay(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            ItemStack stack,
            CallbackInfo ci
    ) {
        CherryHouseMapRenderer.renderTargetIcon(poseStack, submitNodeCollector, packedLight, stack);
    }
}
