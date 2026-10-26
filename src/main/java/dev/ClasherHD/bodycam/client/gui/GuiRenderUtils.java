package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class GuiRenderUtils {

    private GuiRenderUtils() {
    }

    public static void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int rCW, int rCH, int uvCW, int uvCH, int texW, int texH) {
        int cW = Math.min(rCW, width / 2);
        int cH = Math.min(rCH, height / 2);
        graphics.blit(texture, x, y, cW, cH, 0, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - cW, y, cW, cH, texW - uvCW, 0, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + height - cH, cW, cH, 0, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + width - cW, y + height - cH, cW, cH, texW - uvCW, texH - uvCH, uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + cW, y, width - 2 * cW, cH, uvCW, 0, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x + cW, y + height - cH, width - 2 * cW, cH, uvCW, texH - uvCH, texW - 2 * uvCW, uvCH, texW, texH);
        graphics.blit(texture, x, y + cH, cW, height - 2 * cH, 0, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + width - cW, y + cH, cW, height - 2 * cH, texW - uvCW, uvCH, uvCW, texH - 2 * uvCH, texW, texH);
        graphics.blit(texture, x + cW, y + cH, width - 2 * cW, height - 2 * cH, uvCW, uvCH, texW - 2 * uvCW, texH - 2 * uvCH, texW, texH);
    }
}
