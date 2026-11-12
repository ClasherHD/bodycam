package dev.ClasherHD.bodycam.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ClasherHD.bodycam.block.PodestBlock;
import dev.ClasherHD.bodycam.block.entity.PodestBlockEntity;
import dev.ClasherHD.bodycam.enchantment.EnchantmentLevels;
import dev.ClasherHD.bodycam.item.AnonymizerItem;
import dev.ClasherHD.bodycam.item.ItemDurabilityHelper;
import dev.ClasherHD.bodycam.item.JammerItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class PodestBlockEntityRenderer implements BlockEntityRenderer<PodestBlockEntity> {

    private static final float[][] OUTLINE_OFFSETS = {
            {-0.035F, 0.0F},
            {0.035F, 0.0F},
            {0.0F, -0.035F},
            {0.0F, 0.035F}
    };

    private final ItemRenderer itemRenderer;

    public PodestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PodestBlockEntity podest, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack rawStack = podest.getHeldItem();
        if (rawStack.isEmpty()) {
            return;
        }

        BlockState state = podest.getBlockState();
        Direction facing = state.hasProperty(PodestBlock.FACING) ? state.getValue(PodestBlock.FACING) : Direction.NORTH;
        boolean powered = state.hasProperty(PodestBlock.POWERED) && state.getValue(PodestBlock.POWERED);
        int charges = state.hasProperty(PodestBlock.CHARGES) ? state.getValue(PodestBlock.CHARGES) : podest.getCharges();
        boolean isEmptyItem = ItemDurabilityHelper.isEmpty(rawStack);
        boolean active = powered && charges > 0 && !isEmptyItem;

        ItemStack renderStack = rawStack.copy();
        if (renderStack.getItem() instanceof JammerItem) {
            renderStack.getOrCreateTag().putInt("JammerMode", active ? podest.getEffectiveJammerMode() : 0);
        } else if (renderStack.getItem() instanceof AnonymizerItem) {
            renderStack.getOrCreateTag().putBoolean("AnonymizerActive", active);
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0625F, 0.5F);
        float yRot = 180.0F - facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(67.5F));
        poseStack.translate(0.0F, -0.06F, -0.02F);
        poseStack.scale(0.65F, 0.65F, 0.65F);

        int renderLight = active ? LightTexture.FULL_BRIGHT : packedLight;

        if (!isEmptyItem) {
            int r = 120;
            int g = 120;
            int b = 120;
            int a = active ? 230 : 120;
            if (renderStack.getItem() instanceof JammerItem) {
                if (podest.getEffectiveJammerMode() == 2) {
                    r = 40;
                    g = 150;
                    b = 255;
                } else {
                    r = 0;
                    g = 255;
                    b = 90;
                }
            } else if (renderStack.getItem() instanceof AnonymizerItem) {
                if (EnchantmentLevels.anonymity(renderStack) > 0) {
                    r = 190;
                    g = 60;
                    b = 255;
                } else {
                    r = 0;
                    g = 230;
                    b = 220;
                }
            }

            final int cr = r;
            final int cg = g;
            final int cb = b;
            final int ca = a;

            BakedModel model = this.itemRenderer.getModel(renderStack, podest.getLevel(), null, 0);
            MultiBufferSource tintedSource = renderType -> new TintedOutlineVertexConsumer(
                    bufferSource.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS)),
                    cr, cg, cb, ca
            );

            for (float[] offset : OUTLINE_OFFSETS) {
                poseStack.pushPose();
                poseStack.translate(offset[0], offset[1], 0.004F);
                this.itemRenderer.render(renderStack, ItemDisplayContext.FIXED, false, poseStack, tintedSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                poseStack.popPose();
            }
        }

        this.itemRenderer.renderStatic(renderStack, ItemDisplayContext.FIXED, renderLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, podest.getLevel(), 0);

        poseStack.popPose();
    }

    private static class TintedOutlineVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int r;
        private final int g;
        private final int b;
        private final int a;

        TintedOutlineVertexConsumer(VertexConsumer delegate, int r, int g, int b, int a) {
            this.delegate = delegate;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return delegate.color(this.r, this.g, this.b, this.a);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return delegate.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return delegate.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return delegate.uv2(LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(this.r, this.g, this.b, this.a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
