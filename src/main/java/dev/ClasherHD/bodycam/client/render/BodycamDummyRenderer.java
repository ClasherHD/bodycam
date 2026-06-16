package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class BodycamDummyRenderer<T extends BodycamDummyEntity> extends LivingEntityRenderer<T, PlayerModel<T>> {

    public BodycamDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        HumanoidArmorLayer<T, PlayerModel<T>, net.minecraft.client.model.HumanoidArmorModel<T>> armorLayer = new HumanoidArmorLayer<>(this,
                new net.minecraft.client.model.HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new net.minecraft.client.model.HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager());
        this.addLayer(armorLayer);
        this.addLayer(new dev.ClasherHD.bodycam.client.render.GlowingArmorTrimLayer<>(this, armorLayer));
        this.addLayer(new BodycamItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(T pLivingEntity, com.mojang.blaze3d.vertex.PoseStack pMatrixStack, float pPartialTick) {
        pMatrixStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        this.setModelProperties(entity);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void setModelProperties(T entity) {
        PlayerModel<T> model = this.getModel();
        model.crouching = entity.isCrouching();
        model.rightArmPose = getArmPose(entity, net.minecraft.world.InteractionHand.MAIN_HAND);
        model.leftArmPose = getArmPose(entity, net.minecraft.world.InteractionHand.OFF_HAND);
    }

    private net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(T entity, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        } else {
            if (stack.getItem() instanceof dev.ClasherHD.bodycam.item.BodycamMonitorItem) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.SPYGLASS;
            }
            return net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (entity.getOwnerUUID() != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection()
                        .getPlayerInfo(entity.getOwnerUUID());
                if (info != null) {
                    return info.getSkinLocation();
                }
            }
            return DefaultPlayerSkin.getDefaultSkin(entity.getOwnerUUID());
        }
        return DefaultPlayerSkin.getDefaultSkin(net.minecraft.Util.NIL_UUID);
    }
}
