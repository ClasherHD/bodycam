package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class ScholarRenderer extends MobRenderer<ScholarEntity, PlayerModel<ScholarEntity>> {

    public static final ResourceLocation TEX_COMMON = bodycam.id("textures/entity/scholar_common.png");
    public static final ResourceLocation TEX_UNCOMMON = bodycam.id("textures/entity/scholar_uncommon.png");
    public static final ResourceLocation TEX_RARE = bodycam.id("textures/entity/scholar_rare.png");
    public static final ResourceLocation TEX_RARE2 = bodycam.id("textures/entity/scholar_rare2.png");
    public static final ResourceLocation TEX_ULTRA_RARE = bodycam.id("textures/entity/scholar_ultra_rare.png");

    private final PlayerModel<ScholarEntity> classicModel;
    private final PlayerModel<ScholarEntity> slimModel;

    public ScholarRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        this.classicModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = this.model;
        this.addLayer(new BodycamItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(ScholarEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void render(ScholarEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isSlimModel() ? this.slimModel : this.classicModel;
        this.model.crouching = entity.isCrouching();
        this.model.rightArmPose = getArmPose(entity, InteractionHand.MAIN_HAND);
        this.model.leftArmPose = getArmPose(entity, InteractionHand.OFF_HAND);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private HumanoidModel.ArmPose getArmPose(ScholarEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        return stack.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public ResourceLocation getTextureLocation(ScholarEntity entity) {
        return getTextureForVariant(entity.getVariant());
    }

    public static ResourceLocation getTextureForVariant(String variant) {
        return switch (ScholarEntity.normalizeVariant(variant)) {
            case ScholarEntity.VARIANT_UNCOMMON -> TEX_UNCOMMON;
            case ScholarEntity.VARIANT_RARE -> TEX_RARE;
            case ScholarEntity.VARIANT_RARE2 -> TEX_RARE2;
            case ScholarEntity.VARIANT_ULTRA_RARE -> TEX_ULTRA_RARE;
            default -> TEX_COMMON;
        };
    }
}
