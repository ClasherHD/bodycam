package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.entity.ScholarEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public class ScholarRenderer extends MobRenderer<ScholarEntity, AvatarRenderState, PlayerModel> {

    public static final Identifier TEX_COMMON = bodycam.id("textures/entity/scholar_common.png");
    public static final Identifier TEX_UNCOMMON = bodycam.id("textures/entity/scholar_uncommon.png");
    public static final Identifier TEX_RARE = bodycam.id("textures/entity/scholar_rare.png");
    public static final Identifier TEX_RARE2 = bodycam.id("textures/entity/scholar_rare2.png");
    public static final Identifier TEX_ULTRA_RARE = bodycam.id("textures/entity/scholar_ultra_rare.png");

    private final PlayerModel classicModel;
    private final PlayerModel slimModel;

    public ScholarRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        this.classicModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = this.model;
        this.addLayer(new BodycamItemInHandLayer<>(this));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(ScholarEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidRenderHelper.populateAvatarState(entity, state, this.itemModelResolver, partialTick);
        state.headEquipment = ItemStack.EMPTY;
        state.chestEquipment = ItemStack.EMPTY;
        state.legsEquipment = ItemStack.EMPTY;
        state.feetEquipment = ItemStack.EMPTY;
        Identifier texture = getTextureForVariant(entity.getVariant());
        state.skin = HumanoidRenderHelper.createStaticSkin(texture, entity.isSlimModel());
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void submit(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        PlayerModel previousModel = this.model;
        this.model = state.skin != null && state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.classicModel;
        try {
            super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
        } finally {
            this.model = previousModel;
        }
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin != null ? state.skin.body().texturePath() : TEX_COMMON;
    }

    public static Identifier getTextureForVariant(String variant) {
        return switch (ScholarEntity.normalizeVariant(variant)) {
            case ScholarEntity.VARIANT_UNCOMMON -> TEX_UNCOMMON;
            case ScholarEntity.VARIANT_RARE -> TEX_RARE;
            case ScholarEntity.VARIANT_RARE2 -> TEX_RARE2;
            case ScholarEntity.VARIANT_ULTRA_RARE -> TEX_ULTRA_RARE;
            default -> TEX_COMMON;
        };
    }
}
