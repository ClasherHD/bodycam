package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static void drawNineSlice(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height,
                                     int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        drawNineSlice(graphics, texture, x, y, width, height, rCW, rCH, uvCW, uvCH, texW, texH, 0xFFFFFFFF);
    }

    public static void drawNineSlice(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height,
                                     int rCW, int rCH, int uvCW, int uvCH, int texW, int texH, int color) {
        int cW = Math.min(rCW, width / 2);
        int cH = Math.min(rCH, height / 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, cW, cH, uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cW, y, (float) (texW - uvCW), 0.0F, cW, cH, uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - cH, 0.0F, (float) (texH - uvCH), cW, cH, uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cW, y + height - cH, (float) (texW - uvCW), (float) (texH - uvCH), cW, cH, uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cW, y, (float) uvCW, 0.0F, width - 2 * cW, cH, texW - 2 * uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cW, y + height - cH, (float) uvCW, (float) (texH - uvCH), width - 2 * cW, cH, texW - 2 * uvCW, uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + cH, 0.0F, (float) uvCH, cW, height - 2 * cH, uvCW, texH - 2 * uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - cW, y + cH, (float) (texW - uvCW), (float) uvCH, cW, height - 2 * cH, uvCW, texH - 2 * uvCH, texW, texH, color);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + cW, y + cH, (float) uvCW, (float) uvCH, width - 2 * cW, height - 2 * cH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH, color);
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static int applyAlpha(int rgbOrArgb, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (clampedAlpha << 24) | (rgbOrArgb & 0x00FFFFFF);
    }
}
