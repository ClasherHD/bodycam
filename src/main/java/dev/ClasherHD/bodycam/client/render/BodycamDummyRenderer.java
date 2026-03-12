package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class BodycamDummyRenderer extends LivingEntityRenderer<BodycamDummyEntity, PlayerModel<BodycamDummyEntity>> {

    public BodycamDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new net.minecraft.client.model.HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new net.minecraft.client.model.HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(BodycamDummyEntity pLivingEntity, com.mojang.blaze3d.vertex.PoseStack pMatrixStack, float pPartialTick) {
        pMatrixStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(BodycamDummyEntity entity) {
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
        return DefaultPlayerSkin.getDefaultSkin(java.util.UUID.randomUUID());
    }
}
