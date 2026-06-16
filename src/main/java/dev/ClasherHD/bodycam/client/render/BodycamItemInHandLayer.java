package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("null")
public class BodycamItemInHandLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel & HeadedModel> extends ItemInHandLayer<T, M> {
    private final ItemInHandRenderer itemInHandRenderer;

    public BodycamItemInHandLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer itemInHandRenderer) {
        super(parent, itemInHandRenderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        boolean isMonitor = stack.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem;
        boolean isSpyglass = stack.is(Items.SPYGLASS);
        boolean shouldRenderAtEye = false;
        if (isMonitor || isSpyglass) {
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                shouldRenderAtEye = entity.getUseItem() == stack && entity.swingTime == 0;
            } else {
                shouldRenderAtEye = entity.swingTime == 0;
            }
        }

        if (shouldRenderAtEye) {
            this.renderArmWithSpyglass(entity, stack, arm, poseStack, buffer, packedLight);
        } else {
            super.renderArmWithItem(entity, stack, displayContext, arm, poseStack, buffer, packedLight);
        }
    }

    private void renderArmWithSpyglass(LivingEntity entity, ItemStack stack, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        ModelPart head = this.getParentModel().getHead();
        float originalXRot = head.xRot;
        head.xRot = Mth.clamp(head.xRot, -0.5235988F, 1.5707964F);
        head.translateAndRotate(poseStack);
        head.xRot = originalXRot;
        net.minecraft.client.renderer.entity.layers.CustomHeadLayer.translateToHead(poseStack, false);
        boolean isLeftHand = arm == HumanoidArm.LEFT;
        poseStack.translate((isLeftHand ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
        this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.HEAD, false, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
