package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.CherryHouseExplorerMapItem;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;

public final class CherryHouseMapRenderer {

    private static final String TAG_CENTER_X = "CherryHouseCenterX";
    private static final String TAG_CENTER_Z = "CherryHouseCenterZ";

    private static final RenderType MAP_BACKGROUND = RenderType.text(
            ResourceLocation.withDefaultNamespace("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.text(
            ResourceLocation.withDefaultNamespace("textures/map/map_background_checkerboard.png"));
    private static final RenderType CHERRY_HOUSE_ICON = RenderType.text(
            bodycam.id("textures/map/cherry_house_icon.png"));

    private CherryHouseMapRenderer() {
    }

    public static boolean renderHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            InteractionHand hand, float swingProgress, float equipProgress, float pitch, ItemStack stack) {
        if (!(stack.getItem() instanceof CherryHouseExplorerMapItem)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }

        poseStack.pushPose();
        if (hand == InteractionHand.MAIN_HAND && mc.player.getOffhandItem().isEmpty()) {
            renderTwoHandedMap(mc, poseStack, buffer, packedLight, pitch, equipProgress, swingProgress, stack);
        } else {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                    ? mc.player.getMainArm()
                    : mc.player.getMainArm().getOpposite();
            renderOneHandedMap(mc, poseStack, buffer, packedLight, equipProgress, arm, swingProgress, stack);
        }
        poseStack.popPose();
        return true;
    }

    public static boolean renderItemInFrame(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemStack stack) {
        if (!(stack.getItem() instanceof CherryHouseExplorerMapItem)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        MapId mapId = stack.get(DataComponents.MAP_ID);
        MapItemSavedData savedData = mapId != null ? MapItem.getSavedData(mapId, mc.level) : null;
        if (mapId == null || savedData == null) {
            return false;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        poseStack.translate(-64.0F, -64.0F, 0.0F);
        poseStack.translate(0.0F, 0.0F, -1.0F);
        mc.gameRenderer.getMapRenderer().render(poseStack, buffer, mapId, savedData, true, packedLight);
        drawTargetIcon(poseStack, buffer, stack, savedData, packedLight);
        poseStack.popPose();
        return true;
    }

    private static void renderOneHandedMap(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            float equipProgress, HumanoidArm arm, float swingProgress, ItemStack stack) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * 0.125F, -0.125F, 0.0F);
        if (!mc.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 10.0F));
            renderPlayerArm(mc, poseStack, buffer, packedLight, equipProgress, swingProgress, arm);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(side * 0.51F, -0.08F + equipProgress * -1.2F, -0.75F);
        float sqrtSwing = Mth.sqrt(swingProgress);
        float sinSqrt = Mth.sin(sqrtSwing * (float) Math.PI);
        poseStack.translate(side * -0.5F * sinSqrt, 0.4F * Mth.sin(sqrtSwing * ((float) Math.PI * 2F)) - 0.3F * sinSqrt,
                -0.3F * Mth.sin(swingProgress * (float) Math.PI));
        poseStack.mulPose(Axis.XP.rotationDegrees(sinSqrt * -45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * sinSqrt * -30.0F));
        renderMapQuad(mc, poseStack, buffer, packedLight, stack);
        poseStack.popPose();
    }

    private static void renderTwoHandedMap(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            float pitch, float equipProgress, float swingProgress, ItemStack stack) {
        float sqrtSwing = Mth.sqrt(swingProgress);
        float yOff = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float zOff = -0.4F * Mth.sin(sqrtSwing * (float) Math.PI);
        poseStack.translate(0.0F, -yOff / 2.0F, zOff);
        float tilt = Mth.clamp(1.0F - pitch / 45.0F + 0.1F, 0.0F, 1.0F);
        float tiltCos = -Mth.cos(tilt * (float) Math.PI) * 0.5F + 0.5F;
        poseStack.translate(0.0F, 0.04F + equipProgress * -1.2F + tiltCos * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltCos * -85.0F));
        if (!mc.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            renderMapHand(mc, poseStack, buffer, packedLight, HumanoidArm.RIGHT);
            renderMapHand(mc, poseStack, buffer, packedLight, HumanoidArm.LEFT);
            poseStack.popPose();
        }
        float swingAngle = Mth.sin(sqrtSwing * (float) Math.PI);
        poseStack.mulPose(Axis.XP.rotationDegrees(swingAngle * 20.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);
        renderMapQuad(mc, poseStack, buffer, packedLight, stack);
    }

    private static void renderMapHand(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, int packedLight, HumanoidArm arm) {
        RenderSystem.setShaderTexture(0, mc.player.getSkin().texture());
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(mc.player);
        poseStack.pushPose();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        poseStack.translate(side * 0.3F, -1.1F, 0.45F);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffer, packedLight, mc.player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, packedLight, mc.player);
        }
        poseStack.popPose();
    }

    private static void renderPlayerArm(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            float equipProgress, float swingProgress, HumanoidArm arm) {
        boolean right = arm != HumanoidArm.LEFT;
        float side = right ? 1.0F : -1.0F;
        float sqrtSwing = Mth.sqrt(swingProgress);
        float x = -0.3F * Mth.sin(sqrtSwing * (float) Math.PI);
        float y = 0.4F * Mth.sin(sqrtSwing * ((float) Math.PI * 2F));
        float z = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(side * (x + 0.64000005F), y + -0.6F + equipProgress * -0.6F, z + -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        float sinSq = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float sinSqrt = Mth.sin(sqrtSwing * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * sinSqrt * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * sinSq * -20.0F));
        RenderSystem.setShaderTexture(0, mc.player.getSkin().texture());
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(mc.player);
        if (right) {
            renderer.renderRightHand(poseStack, buffer, packedLight, mc.player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, packedLight, mc.player);
        }
    }

    private static void renderMapQuad(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemStack stack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);

        MapId mapId = stack.get(DataComponents.MAP_ID);
        MapItemSavedData savedData = mapId != null ? MapItem.getSavedData(mapId, mc.level) : null;
        VertexConsumer bg = buffer.getBuffer(savedData == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
        Matrix4f pose = poseStack.last().pose();
        bg.addVertex(pose, -7.0F, 135.0F, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(packedLight);
        bg.addVertex(pose, 135.0F, 135.0F, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(packedLight);
        bg.addVertex(pose, 135.0F, -7.0F, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(packedLight);
        bg.addVertex(pose, -7.0F, -7.0F, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(packedLight);

        if (savedData != null) {
            mc.gameRenderer.getMapRenderer().render(poseStack, buffer, mapId, savedData, false, packedLight);
            drawTargetIcon(poseStack, buffer, stack, savedData, packedLight);
        }
    }

    private static void drawTargetIcon(PoseStack poseStack, MultiBufferSource buffer, ItemStack stack,
            MapItemSavedData savedData, int packedLight) {
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null || !tag.contains(CherryHouseExplorerMapItem.TAG_TARGET_X)
                || !tag.contains(CherryHouseExplorerMapItem.TAG_TARGET_Z)) {
            return;
        }
        int targetX = tag.getInt(CherryHouseExplorerMapItem.TAG_TARGET_X);
        int targetZ = tag.getInt(CherryHouseExplorerMapItem.TAG_TARGET_Z);
        int scaleFactor = 1 << savedData.scale;
        int scaleBlocks = 128 * scaleFactor;
        int centerX = tag.contains(TAG_CENTER_X, 99)
                ? tag.getInt(TAG_CENTER_X)
                : Mth.floor((targetX + 64.0D) / (double) scaleBlocks) * scaleBlocks + scaleBlocks / 2 - 64;
        int centerZ = tag.contains(TAG_CENTER_Z, 99)
                ? tag.getInt(TAG_CENTER_Z)
                : Mth.floor((targetZ + 64.0D) / (double) scaleBlocks) * scaleBlocks + scaleBlocks / 2 - 64;
        float px = Mth.clamp((targetX - centerX) / (float) scaleFactor + 64.0F, 5.0F, 123.0F);
        float py = Mth.clamp((targetZ - centerZ) / (float) scaleFactor + 64.0F, 5.0F, 123.0F);

        poseStack.pushPose();
        poseStack.translate(px, py, -0.025F);
        float halfSize = 5.0F;
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(CHERRY_HOUSE_ICON);
        vc.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(packedLight);
        vc.addVertex(pose, halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(packedLight);
        vc.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(packedLight);
        vc.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(packedLight);
        poseStack.popPose();
    }
}
