package dev.ClasherHD.bodycam.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GuiRenderUtilsTest {
    @Test
    void applyAlpha_clampsAlphaAndPreservesRgbBits() {
        int result = GuiRenderUtils.applyAlpha(0x00123456, 128);
        assertThat(result).isEqualTo(0x80123456);

        int clampedHigh = GuiRenderUtils.applyAlpha(0xFFABCDEF, 500);
        assertThat(clampedHigh).isEqualTo(0xFFABCDEF);

        int clampedLow = GuiRenderUtils.applyAlpha(0xFFABCDEF, -20);
        assertThat(clampedLow).isEqualTo(0x00ABCDEF);
    }

    @Test
    void drawBorder_ignoresNonPositiveDimensionsAndDrawsFourEdges() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        GuiRenderUtils.drawBorder(graphics, 10, 20, 0, 10, 0xFFFFFFFF);
        GuiRenderUtils.drawBorder(graphics, 10, 20, 10, -5, 0xFFFFFFFF);
        verifyNoInteractions(graphics);

        GuiRenderUtils.drawBorder(graphics, 10, 20, 30, 40, 0xFF00FF00);
        verify(graphics).fill(10, 20, 40, 21, 0xFF00FF00);
        verify(graphics).fill(10, 59, 40, 60, 0xFF00FF00);
        verify(graphics).fill(10, 20, 11, 60, 0xFF00FF00);
        verify(graphics).fill(39, 20, 40, 60, 0xFF00FF00);
    }
}
