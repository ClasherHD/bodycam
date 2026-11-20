package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.item.PortableNetherPortalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings("null")
public final class PortalChannelOverlay {

    private static float lastAlpha = 0.0F;

    private PortalChannelOverlay() {
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            lastAlpha = 0.0F;
            return;
        }

        if (!mc.player.isUsingItem()) {
            lastAlpha = Math.max(0.0F, lastAlpha - 0.1F);
            if (lastAlpha <= 0.01F) {
                lastAlpha = 0.0F;
                return;
            }
            drawNetherPortalOverlay(mc, graphics, lastAlpha);
            return;
        }

        ItemStack usedItem = mc.player.getUseItem();
        if (!(usedItem.getItem() instanceof PortableNetherPortalItem)) {
            lastAlpha = 0.0F;
            return;
        }

        int total = usedItem.getUseDuration(mc.player);
        if (total <= 0) {
            lastAlpha = 0.0F;
            return;
        }

        int remaining = mc.player.getUseItemRemainingTicks();
        float elapsed = (total - remaining) + partialTick;
        float progress = Mth.clamp(elapsed / (float) total, 0.0F, 1.0F);
        float alpha = Mth.clamp(progress * 0.9F, 0.05F, 0.9F);
        lastAlpha = alpha;

        drawNetherPortalOverlay(mc, graphics, alpha);
    }

    private static void drawNetherPortalOverlay(Minecraft mc, GuiGraphics graphics, float alpha) {
        TextureAtlasSprite sprite = mc.getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                0,
                0,
                graphics.guiWidth(),
                graphics.guiHeight(),
                ARGB.white(alpha)
        );
    }
}
