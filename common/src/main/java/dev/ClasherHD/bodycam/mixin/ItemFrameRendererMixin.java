package dev.ClasherHD.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.render.CherryHouseFrameStateAccess;
import dev.ClasherHD.bodycam.client.render.CherryHouseMapRenderer;
import dev.ClasherHD.bodycam.item.CherryHouseExplorerMapItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin<T extends ItemFrame> {
    @Shadow
    private int getLightCoords(boolean isGlowFrame, int glowLightVal, int regularLightVal) {
        throw new AssertionError();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void bodycam$extractFramedItem(T entity, ItemFrameRenderState renderState, float partialTick, CallbackInfo ci) {
        ItemStack framed = entity.getItem();
        ((CherryHouseFrameStateAccess) renderState).bodycam$setFramedItem(
                framed.getItem() instanceof CherryHouseExplorerMapItem ? framed.copy() : ItemStack.EMPTY
        );
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MapRenderer;render(Lnet/minecraft/client/renderer/state/MapRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ZI)V",
                    shift = At.Shift.AFTER
            )
    )
    private void bodycam$renderFramedMapOverlay(
            ItemFrameRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        ItemStack stack = ((CherryHouseFrameStateAccess) renderState).bodycam$getFramedItem();
        if (stack != null && !stack.isEmpty()) {
            int light = this.getLightCoords(renderState.isGlowFrame, 15728850, renderState.lightCoords);
            CherryHouseMapRenderer.renderTargetIcon(poseStack, submitNodeCollector, light, stack);
        }
    }
}
