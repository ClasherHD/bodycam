package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

public class BodycamDummyRenderer<T extends BodycamDummyEntity> extends LivingEntityRenderer<T, BodycamDummyRenderer.DummyRenderState, PlayerModel> {
    private static final java.util.UUID FALLBACK_UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static class DummyRenderState extends net.minecraft.client.renderer.entity.state.AvatarRenderState {
        public java.util.UUID ownerUUID;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BodycamDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HumanoidArmorLayer(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(
                        net.minecraft.client.model.geom.ModelLayers.PLAYER_ARMOR,
                        context.getModelSet(),
                        net.minecraft.client.model.HumanoidModel::new),
                context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer(this));
        this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public DummyRenderState createRenderState() {
        return new DummyRenderState();
    }

    @Override
    public void extractRenderState(T entity, DummyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ownerUUID = entity.getOwnerUUID();
        if (state.ownerUUID != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection()
                        .getPlayerInfo(state.ownerUUID);
                if (info != null) {
                    state.skin = info.getSkin();
                } else {
                    state.skin = DefaultPlayerSkin.get(state.ownerUUID);
                }
            } else {
                state.skin = DefaultPlayerSkin.get(state.ownerUUID);
            }
        } else {
            state.skin = DefaultPlayerSkin.get(FALLBACK_UUID);
        }
        state.chestEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        state.legsEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        state.feetEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
        state.headEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
    }

    @Override
    protected void scale(DummyRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTextureLocation(DummyRenderState state) {
        if (state.ownerUUID != null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection()
                        .getPlayerInfo(state.ownerUUID);
                if (info != null) {
                    return info.getSkin().body().texturePath();
                }
            }
            return DefaultPlayerSkin.get(state.ownerUUID).body().texturePath();
        }
        return DefaultPlayerSkin.get(FALLBACK_UUID).body().texturePath();
    }
}
