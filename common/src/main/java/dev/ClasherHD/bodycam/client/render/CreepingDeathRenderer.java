package dev.ClasherHD.bodycam.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ClasherHD.bodycam.entity.CreepingDeathEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public class CreepingDeathRenderer extends MobRenderer<CreepingDeathEntity, PlayerModel<CreepingDeathEntity>> {

    private static final Map<UUID, GameProfile> RESOLVED_PROFILES = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING_PROFILES = ConcurrentHashMap.newKeySet();

    private final PlayerModel<CreepingDeathEntity> classicModel;
    private final PlayerModel<CreepingDeathEntity> slimModel;

    public CreepingDeathRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.classicModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        HumanoidArmorLayer<CreepingDeathEntity, PlayerModel<CreepingDeathEntity>, HumanoidArmorModel<CreepingDeathEntity>> armorLayer =
                new HumanoidArmorLayer<>(
                        this,
                        new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                        context.getModelManager()
                );
        this.addLayer(armorLayer);
        this.addLayer(new GlowingArmorTrimLayer<>(this));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
        this.addLayer(new BodycamItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(CreepingDeathEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected void setupRotations(CreepingDeathEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTicks, float scale) {
        float swimAmount = entity.getSwimAmount(partialTicks);
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTicks, scale);
        if (entity.isFallFlying()) {
            float ticks = (float) entity.getFallFlyingTicks() + partialTicks;
            float factor = Mth.clamp(ticks * ticks / 100.0F, 0.0F, 1.0F);
            if (!entity.isAutoSpinAttack()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(factor * (-90.0F - entity.getXRot())));
            }
            Vec3 viewVec = entity.getViewVector(partialTicks);
            Vec3 delta = entity.getDeltaMovement();
            double d0 = delta.horizontalDistanceSqr();
            double d1 = viewVec.horizontalDistanceSqr();
            if (d0 > 0.0D && d1 > 0.0D) {
                double dot = (delta.x * viewVec.x + delta.z * viewVec.z) / Math.sqrt(d0 * d1);
                double cross = delta.x * viewVec.z - delta.z * viewVec.x;
                poseStack.mulPose(Axis.YP.rotation((float) (Math.signum(cross) * Math.acos(Math.min(1.0D, Math.max(-1.0D, dot))))));
            }
        } else if (swimAmount > 0.0F) {
            float targetPitch = entity.isInWater() ? -90.0F - entity.getXRot() : -90.0F;
            float pitch = Mth.lerp(swimAmount, 0.0F, targetPitch);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            if (entity.isVisuallySwimming()) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }

    @Override
    public void render(CreepingDeathEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = entity.isSlimModel() ? this.slimModel : this.classicModel;
        PlayerModel<CreepingDeathEntity> model = this.getModel();
        model.crouching = entity.isCrouching();
        model.rightArmPose = getArmPose(entity, InteractionHand.MAIN_HAND);
        model.leftArmPose = getArmPose(entity, InteractionHand.OFF_HAND);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private HumanoidModel.ArmPose getArmPose(CreepingDeathEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        return stack.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public ResourceLocation getTextureLocation(CreepingDeathEntity entity) {
        String scholarSkin = entity.getScholarSkin();
        if (scholarSkin != null && !scholarSkin.isEmpty()) {
            return ScholarRenderer.getTextureForVariant(scholarSkin);
        }
        return getSkinForPlayer(entity.getSkinUUID(), entity.getSkinName());
    }

    public static ResourceLocation getSkinForPlayer(UUID uuid, String name) {
        if (uuid == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        GameProfile resolved = RESOLVED_PROFILES.get(uuid);
        if (resolved != null) {
            return mc.getSkinManager().getInsecureSkin(resolved).texture();
        }
        if (name != null && !name.isEmpty() && PENDING_PROFILES.add(uuid)) {
            Util.backgroundExecutor().execute(() -> {
                try {
                    ProfileResult result = mc.getMinecraftSessionService().fetchProfile(uuid, false);
                    GameProfile profile = result != null ? result.profile() : new GameProfile(uuid, name);
                    RESOLVED_PROFILES.put(uuid, profile);
                } catch (Exception ignored) {
                } finally {
                    PENDING_PROFILES.remove(uuid);
                }
            });
        }
        return DefaultPlayerSkin.get(uuid).texture();
    }
}
