package dev.ClasherHD.bodycam.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ClasherHD.bodycam.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public final class HumanoidRenderHelper {
    private static final Map<UUID, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SKIN_LOADING = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> STATIC_SKIN_CACHE = new ConcurrentHashMap<>();

    private HumanoidRenderHelper() {
    }

    public static void clearCache() {
        SKIN_CACHE.clear();
        SKIN_LOADING.clear();
    }

    public static boolean isSpyglassOrMonitor(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.SPYGLASS) || stack.is(ModItems.BODYCAM_MONITOR.get()));
    }

    public static void populateAvatarState(LivingEntity entity, AvatarRenderState state,
            ItemModelResolver itemModelResolver, float partialTick) {
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, itemModelResolver);
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
        state.fallFlyingTimeInTicks = entity.getFallFlyingTicks() + partialTick;
        state.shouldApplyFlyingYRot = false;
        state.flyingYRot = 0.0F;
        state.leftArmPose = getArmPose(entity, HumanoidArm.LEFT);
        state.rightArmPose = getArmPose(entity, HumanoidArm.RIGHT);

        state.heldOnHead.clear();
        if (entity.isUsingItem()) {
            ItemStack useItem = entity.getUseItem();
            if (isSpyglassOrMonitor(useItem)) {
                itemModelResolver.updateForLiving(state.heldOnHead, useItem, ItemDisplayContext.HEAD, entity);
            }
        }
    }

    public static HumanoidModel.ArmPose getArmPose(LivingEntity entity, HumanoidArm arm) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
            ItemUseAnimation anim = stack.getUseAnimation();
            if (anim == ItemUseAnimation.BLOCK) {
                return HumanoidModel.ArmPose.BLOCK;
            }
            if (anim == ItemUseAnimation.BOW) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
            if (anim == ItemUseAnimation.TRIDENT) {
                return HumanoidModel.ArmPose.THROW_TRIDENT;
            }
            if (anim == ItemUseAnimation.SPEAR) {
                return HumanoidModel.ArmPose.SPEAR;
            }
            if (anim == ItemUseAnimation.CROSSBOW) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (anim == ItemUseAnimation.SPYGLASS || isSpyglassOrMonitor(stack)) {
                return HumanoidModel.ArmPose.SPYGLASS;
            }
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    public static PlayerSkin resolvePlayerSkin(@Nullable UUID uuid, @Nullable String name, @Nullable Boolean slimHint) {
        if (uuid == null) {
            PlayerSkin base = DefaultPlayerSkin.getDefaultSkin();
            return applySlimHint(base, slimHint);
        }

        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) {
                PlayerSkin liveSkin = info.getSkin();
                SKIN_CACHE.put(uuid, liveSkin);
                return liveSkin;
            }
        }

        PlayerSkin cached = SKIN_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (name != null && !name.isBlank() && SKIN_LOADING.putIfAbsent(uuid, Boolean.TRUE) == null) {
            GameProfile profile = new GameProfile(uuid, name);
            mc.getSkinManager().get(profile).thenAccept(opt -> {
                if (opt.isPresent()) {
                    SKIN_CACHE.put(uuid, opt.get());
                }
                SKIN_LOADING.remove(uuid);
            }).exceptionally(err -> {
                SKIN_LOADING.remove(uuid);
                return null;
            });
        }

        return applySlimHint(DefaultPlayerSkin.get(uuid), slimHint);
    }

    public static PlayerSkin createStaticSkin(Identifier texture, boolean slim) {
        String key = texture.toString() + (slim ? ":slim" : ":wide");
        return STATIC_SKIN_CACHE.computeIfAbsent(key, k -> {
            ClientAsset.ResourceTexture asset = new ClientAsset.ResourceTexture(texture, texture);
            return new PlayerSkin(asset, null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, true);
        });
    }

    public static boolean applyPlayerRotations(AvatarRenderState state, PoseStack poseStack) {
        float swimAmount = state.swimAmount;
        float xRot = state.xRot;
        if (state.isFallFlying) {
            float scale = state.fallFlyingScale();
            if (!state.isAutoSpinAttack) {
                poseStack.mulPose(Axis.XP.rotationDegrees(scale * (-90.0F - xRot)));
            }
            if (state.shouldApplyFlyingYRot) {
                poseStack.mulPose(Axis.YP.rotation(state.flyingYRot));
            }
            return true;
        }
        if (swimAmount > 0.0F) {
            float target = state.isInWater ? -90.0F - xRot : -90.0F;
            float lerped = Mth.lerp(swimAmount, 0.0F, target);
            poseStack.mulPose(Axis.XP.rotationDegrees(lerped));
            if (state.isVisuallySwimming) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
            return true;
        }
        return false;
    }

    private static PlayerSkin applySlimHint(PlayerSkin skin, @Nullable Boolean slimHint) {
        if (slimHint == null) {
            return skin;
        }
        PlayerModelType desired = slimHint ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        if (skin.model() == desired) {
            return skin;
        }
        return new PlayerSkin(skin.body(), skin.cape(), skin.elytra(), desired, skin.secure());
    }
}
