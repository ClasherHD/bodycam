package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.entity.BodycamDummyEntity;
import dev.ClasherHD.bodycam.entity.HologramDummyEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerModelType;

@SuppressWarnings("null")
public class BodycamDummyRenderer<T extends BodycamDummyEntity>
        extends LivingEntityRenderer<T, AvatarRenderState, PlayerModel> {

    private static final int HOLOGRAM_TINT = ARGB.color(145, 95, 220, 255);

    private final PlayerModel classicModel;
    private final PlayerModel slimModel;

    public BodycamDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.classicModel = this.model;
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                        part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()));
        this.addLayer(new GlowingArmorTrimLayer<>(this, context.getModelSet()));
        this.addLayer(new BodycamItemInHandLayer<>(this));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (BodycamViewScreen.isMonitoring && livingEntity.getUUID().equals(BodycamViewScreen.targetUuid)) {
            return false;
        }
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }

    @Override
    public void extractRenderState(T entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidRenderHelper.populateAvatarState(entity, state, this.itemModelResolver, partialTick);
        state.skin = HumanoidRenderHelper.resolvePlayerSkin(entity.getOwnerUuidOrNull(), entity.getOwnerName(), null);
        BodycamHologramState hologramState = (BodycamHologramState) (Object) state;
        hologramState.bodycam$setHologram(entity instanceof HologramDummyEntity);
        hologramState.bodycam$setPartialTick(partialTick);
        hologramState.bodycam$setGameTime(entity.level() != null ? entity.level().getGameTime() : 0L);
    }

    @Override
    protected float getShadowRadius(AvatarRenderState state) {
        return ((BodycamHologramState) (Object) state).bodycam$isHologram() ? 0.0F : super.getShadowRadius(state);
    }

    @Override
    protected RenderType getRenderType(
            AvatarRenderState state,
            boolean isVisible,
            boolean renderTranslucent,
            boolean appearsGlowing) {
        if (((BodycamHologramState) (Object) state).bodycam$isHologram()) {
            return RenderTypes.entityTranslucent(this.getTextureLocation(state));
        }
        return super.getRenderType(state, isVisible, renderTranslucent, appearsGlowing);
    }

    @Override
    protected int getModelTint(AvatarRenderState state) {
        return ((BodycamHologramState) (Object) state).bodycam$isHologram() ? HOLOGRAM_TINT : super.getModelTint(state);
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        BodycamHologramState hologramState = (BodycamHologramState) (Object) state;
        if (hologramState.bodycam$isHologram()) {
            float bob = (float) Math
                    .sin((hologramState.bodycam$getGameTime() + hologramState.bodycam$getPartialTick()) * 0.1D) * 0.04F;
            poseStack.translate(0.0F, bob, 0.0F);
        }
    }

    @Override
    protected void setupRotations(AvatarRenderState state, PoseStack poseStack, float ageInTicks, float bodyRot) {
        super.setupRotations(state, poseStack, ageInTicks, bodyRot);
        HumanoidRenderHelper.applyPlayerRotations(state, poseStack);
    }

    @Override
    public void submit(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState) {
        PlayerModel previousModel = this.model;
        this.model = state.skin != null && state.skin.model() == PlayerModelType.SLIM ? this.slimModel
                : this.classicModel;
        try {
            super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
        } finally {
            this.model = previousModel;
        }
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin != null ? state.skin.body().texturePath() : DefaultPlayerSkin.getDefaultTexture();
    }
}
