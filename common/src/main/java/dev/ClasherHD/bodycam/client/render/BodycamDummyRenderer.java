package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.item.BodycamMonitorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("null")
public class BodycamDummyRenderer<T extends BodycamDummyEntity> extends LivingEntityRenderer<T, PlayerModel<T>> {
    public BodycamDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        this.addLayer(new GlowingArmorTrimLayer<>(this));
        this.addLayer(new BodycamItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (BodycamViewScreen.isMonitoring && livingEntity.getUUID().equals(BodycamViewScreen.targetUuid)) {
            return false;
        }
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        PlayerModel<T> model = this.getModel();
        model.crouching = entity.isCrouching();
        model.rightArmPose = getArmPose(entity, InteractionHand.MAIN_HAND);
        model.leftArmPose = getArmPose(entity, InteractionHand.OFF_HAND);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static <T extends BodycamDummyEntity> HumanoidModel.ArmPose getArmPose(T entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        return stack.getItem() instanceof BodycamMonitorItem
                ? HumanoidModel.ArmPose.SPYGLASS
                : HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        Optional<UUID> ownerId = entity.getOwnerUUID();
        if (ownerId != null && ownerId.isPresent()) {
            UUID uuid = ownerId.get();
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    return info.getSkin().texture();
                }
            }
            return DefaultPlayerSkin.get(uuid).texture();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
