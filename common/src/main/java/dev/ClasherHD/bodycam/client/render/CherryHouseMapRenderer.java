package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.item.CherryHouseExplorerMapItem;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;

@SuppressWarnings("null")
public final class CherryHouseMapRenderer {

    private static final String TAG_CENTER_X = "CherryHouseCenterX";
    private static final String TAG_CENTER_Z = "CherryHouseCenterZ";

    private static final RenderType CHERRY_HOUSE_ICON = RenderTypes.text(
            bodycam.id("textures/map/cherry_house_icon.png"));

    private CherryHouseMapRenderer() {
    }

    public static void renderTargetIcon(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            int packedLight, ItemStack stack) {
        if (!(stack.getItem() instanceof CherryHouseExplorerMapItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        MapId mapId = stack.get(DataComponents.MAP_ID);
        MapItemSavedData savedData = mapId != null ? MapItem.getSavedData(mapId, mc.level) : null;
        if (savedData == null) {
            return;
        }
        CompoundTag tag = ItemNbtAccess.getReadOnlyTag(stack);
        if (tag == null || !tag.contains(CherryHouseExplorerMapItem.TAG_TARGET_X)
                || !tag.contains(CherryHouseExplorerMapItem.TAG_TARGET_Z)) {
            return;
        }

        int targetX = tag.getIntOr(CherryHouseExplorerMapItem.TAG_TARGET_X, 0);
        int targetZ = tag.getIntOr(CherryHouseExplorerMapItem.TAG_TARGET_Z, 0);
        int scaleFactor = 1 << savedData.scale;
        int scaleBlocks = 128 * scaleFactor;
        int centerX = tag.contains(TAG_CENTER_X)
                ? tag.getIntOr(TAG_CENTER_X, 0)
                : Mth.floor((targetX + 64.0D) / (double) scaleBlocks) * scaleBlocks + scaleBlocks / 2 - 64;
        int centerZ = tag.contains(TAG_CENTER_Z)
                ? tag.getIntOr(TAG_CENTER_Z, 0)
                : Mth.floor((targetZ + 64.0D) / (double) scaleBlocks) * scaleBlocks + scaleBlocks / 2 - 64;
        float px = Mth.clamp((targetX - centerX) / (float) scaleFactor + 64.0F, 5.0F, 123.0F);
        float py = Mth.clamp((targetZ - centerZ) / (float) scaleFactor + 64.0F, 5.0F, 123.0F);

        poseStack.pushPose();
        poseStack.translate(px, py, -0.025F);
        float halfSize = 5.0F;
        submitNodeCollector.submitCustomGeometry(poseStack, CHERRY_HOUSE_ICON, (pose, vc) -> {
            Matrix4f matrix = pose.pose();
            vc.addVertex(matrix, -halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(packedLight);
            vc.addVertex(matrix, halfSize, halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(packedLight);
            vc.addVertex(matrix, halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(packedLight);
            vc.addVertex(matrix, -halfSize, -halfSize, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(packedLight);
        });
        poseStack.popPose();
    }
}
