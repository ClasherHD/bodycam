package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("null")
public class CreepingDeathRenderer extends MobRenderer<CreepingDeathEntity, AvatarRenderState, PlayerModel> {

    private final PlayerModel classicModel;
    private final PlayerModel slimModel;

    public CreepingDeathRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.classicModel = this.model;
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                        part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()));
        this.addLayer(new GlowingArmorTrimLayer<>(this, context.getModelSet()));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new BodycamItemInHandLayer<>(this));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(CreepingDeathEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidRenderHelper.populateAvatarState(entity, state, this.itemModelResolver, partialTick);
        String scholarSkin = entity.getScholarSkin();
        if (scholarSkin != null && !scholarSkin.isEmpty()) {
            state.skin = HumanoidRenderHelper.createStaticSkin(
                    ScholarRenderer.getTextureForVariant(scholarSkin),
                    entity.isSlimModel());
        } else {
            state.skin = HumanoidRenderHelper.resolvePlayerSkin(entity.getSkinUUID(), entity.getSkinName(), null);
        }
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
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

    public static Identifier getSkinForPlayer(@Nullable UUID uuid, @Nullable String name) {
        return HumanoidRenderHelper.resolvePlayerSkin(uuid, name, null).body().texturePath();
    }
}
