package dev.ClasherHD.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.render.CherryHouseMapRenderer;
import dev.architectury.platform.Platform;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void bodycam$renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci) {
        if (Platform.isFabric()
                && CherryHouseMapRenderer.renderHand(
                        poseStack, buffer, combinedLight, hand, swingProgress, equipProgress, pitch, stack)) {
            ci.cancel();
        }
    }
}
