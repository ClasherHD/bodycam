package dev.ClasherHD.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique
    private static final ModelResourceLocation BODYCAM_IN_HAND =
            ModelResourceLocation.inventory(bodycam.id("observation_spyglass/bodycam_monitor_in_hand"));
    @Unique
    private static final ModelResourceLocation BODYCAM_IN_HAND_EMPTY =
            ModelResourceLocation.inventory(bodycam.id("observation_spyglass/bodycam_monitor_in_hand_empty"));

    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private BakedModel bodycam$useInHandModelForPerspectives(
            BakedModel model,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        if (!stack.isEmpty() && stack.getItem() instanceof BodycamMonitorItem) {
            if (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                    || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || displayContext == ItemDisplayContext.HEAD) {
                ModelResourceLocation target = ItemDurabilityHelper.isEmpty(stack)
                        ? BODYCAM_IN_HAND_EMPTY
                        : BODYCAM_IN_HAND;
                BakedModel inHandModel = this.itemModelShaper.getModelManager().getModel(target);
                if (inHandModel != null && inHandModel != this.itemModelShaper.getModelManager().getMissingModel()) {
                    return inHandModel;
                }
            }
        }
        return model;
    }
}
