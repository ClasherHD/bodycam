package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import dev.ClasherHD.bodycam.util.ItemNbtAccess;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class PodestBlockEntityRenderer implements BlockEntityRenderer<PodestBlockEntity, PodestBlockEntityRenderer.PodestRenderState> {

    private static final float[][] OUTLINE_OFFSETS = {
            {-0.035F, 0.0F},
            {0.035F, 0.0F},
            {0.0F, -0.035F},
            {0.0F, 0.035F}
    };

    private final ItemModelResolver itemModelResolver;

    public PodestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public PodestRenderState createRenderState() {
        return new PodestRenderState();
    }

    @Override
    public void extractRenderState(
            PodestBlockEntity podest,
            PodestRenderState state,
            float partialTick,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(podest, state, partialTick, cameraPos, breakProgress);
        state.itemState.clear();
        ItemStack rawStack = podest.getHeldItem();
        if (rawStack.isEmpty()) {
            state.active = false;
            state.outlineColor = 0;
            return;
        }

        BlockState blockState = podest.getBlockState();
        Direction facing = blockState.hasProperty(PodestBlock.FACING) ? blockState.getValue(PodestBlock.FACING) : Direction.NORTH;
        boolean powered = blockState.hasProperty(PodestBlock.POWERED) && blockState.getValue(PodestBlock.POWERED);
        int charges = blockState.hasProperty(PodestBlock.CHARGES) ? blockState.getValue(PodestBlock.CHARGES) : podest.getCharges();
        boolean isEmptyItem = ItemDurabilityHelper.isEmpty(rawStack);
        boolean active = powered && charges > 0 && !isEmptyItem;

        ItemStack renderStack = rawStack.copy();
        if (renderStack.getItem() instanceof JammerItem) {
            int mode = active ? podest.getEffectiveJammerMode() : 0;
            ItemNbtAccess.updateTag(renderStack, tag -> tag.putInt("JammerMode", mode));
        } else if (renderStack.getItem() instanceof AnonymizerItem) {
            ItemNbtAccess.updateTag(renderStack, tag -> tag.putBoolean("AnonymizerActive", active));
        }

        state.yRot = 180.0F - facing.toYRot();
        state.active = active;
        state.lightCoords = podest.getLevel() != null
                ? LevelRenderer.getLightColor(podest.getLevel(), podest.getBlockPos().above())
                : LightTexture.FULL_BRIGHT;
        state.outlineColor = resolveOutlineColor(renderStack, podest, active, isEmptyItem);
        this.itemModelResolver.updateForTopItem(
                state.itemState,
                renderStack,
                ItemDisplayContext.FIXED,
                podest.getLevel(),
                null,
                0
        );
    }

    private static int resolveOutlineColor(ItemStack stack, PodestBlockEntity podest, boolean active, boolean isEmptyItem) {
        if (isEmptyItem) {
            return 0;
        }
        int r = 120;
        int g = 120;
        int b = 120;
        int a = active ? 230 : 120;
        if (stack.getItem() instanceof JammerItem) {
            if (podest.getEffectiveJammerMode() == 2) {
                r = 40;
                g = 150;
                b = 255;
            } else {
                r = 0;
                g = 255;
                b = 90;
            }
        } else if (stack.getItem() instanceof AnonymizerItem) {
            if (EnchantmentLevels.anonymity(stack) > 0) {
                r = 190;
                g = 60;
                b = 255;
            } else {
                r = 0;
                g = 230;
                b = 220;
            }
        }
        return ARGB.color(a, r, g, b);
    }

    @Override
    public void submit(
            PodestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        if (state.itemState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0625F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(67.5F));
        poseStack.translate(0.0F, -0.06F, -0.02F);
        poseStack.scale(0.65F, 0.65F, 0.65F);
        int renderLight = state.active ? LightTexture.FULL_BRIGHT : state.lightCoords;
        if (state.outlineColor != 0) {
            submitGlowOutline(state, poseStack, submitNodeCollector);
        }
        state.itemState.submit(
                poseStack,
                submitNodeCollector,
                renderLight,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    private static void submitGlowOutline(PodestRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        SubmitNodeStorage capture = new SubmitNodeStorage();
        PoseStack identityPose = new PoseStack();
        state.itemState.submit(identityPose, capture, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        float r = ARGB.red(state.outlineColor) / 255.0F;
        float g = ARGB.green(state.outlineColor) / 255.0F;
        float b = ARGB.blue(state.outlineColor) / 255.0F;
        float a = ARGB.alpha(state.outlineColor) / 255.0F;
        for (SubmitNodeCollection collection : capture.getSubmitsPerOrder().values()) {
            for (SubmitNodeStorage.ItemSubmit itemSubmit : collection.getItemSubmits()) {
                List<BakedQuad> quads = itemSubmit.quads();
                if (quads.isEmpty()) {
                    continue;
                }
                Identifier atlas = quads.getFirst().sprite().atlasLocation();
                RenderType emissiveType = RenderTypes.entityTranslucentEmissive(atlas);
                PoseStack.Pose itemTransformPose = itemSubmit.pose();
                for (float[] offset : OUTLINE_OFFSETS) {
                    poseStack.pushPose();
                    poseStack.translate(offset[0], offset[1], 0.004F);
                    submitNodeCollector.submitCustomGeometry(poseStack, emissiveType, (pose, consumer) -> {
                        PoseStack quadPose = new PoseStack();
                        quadPose.last().set(pose);
                        quadPose.last().pose().mul(itemTransformPose.pose());
                        quadPose.last().normal().mul(itemTransformPose.normal());
                        for (BakedQuad quad : quads) {
                            consumer.putBulkData(
                                    quadPose.last(),
                                    quad,
                                    r,
                                    g,
                                    b,
                                    a,
                                    LightTexture.FULL_BRIGHT,
                                    OverlayTexture.NO_OVERLAY
                            );
                        }
                    });
                    poseStack.popPose();
                }
            }
        }
    }

    public static class PodestRenderState extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public float yRot;
        public boolean active;
        public int outlineColor;
    }
}
