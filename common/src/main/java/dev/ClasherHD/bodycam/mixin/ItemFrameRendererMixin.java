package dev.ClasherHD.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.render.CherryHouseMapRenderer;
import dev.architectury.platform.Platform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
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
    private int getLightVal(T itemFrame, int glowLightVal, int regularLightVal) {
        throw new AssertionError();
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 2,
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void bodycam$renderItemInFrame(
            T entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (!Platform.isFabric()) {
            return;
        }
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            int light = this.getLightVal(entity, 15728850, packedLight);
            if (CherryHouseMapRenderer.renderItemInFrame(poseStack, buffer, light, stack)) {
                poseStack.popPose();
                ci.cancel();
            }
        }
    }
}
