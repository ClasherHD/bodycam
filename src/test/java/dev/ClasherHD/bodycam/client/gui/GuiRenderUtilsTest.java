package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
class GuiRenderUtilsTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("drawNineSlice invokes blit 9 times with appropriate dimensions")
    void drawNineSlice_performsNineBlits() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        ResourceLocation texture = new ResourceLocation("bodycam", "test_window");

        GuiRenderUtils.drawNineSlice(graphics, texture, 10, 20, 100, 80, 16, 16, 16, 16, 256, 256);

        verify(graphics, times(9)).blit(
                eq(texture),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyFloat(),
                anyFloat(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt()
        );
    }

    @Test
    @DisplayName("drawNineSlice clamps corner sizes when dimensions are smaller than double corner size")
    void drawNineSlice_clampsSmallDimensions() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        ResourceLocation texture = new ResourceLocation("bodycam", "test_window");

        GuiRenderUtils.drawNineSlice(graphics, texture, 0, 0, 20, 10, 16, 16, 16, 16, 256, 256);

        verify(graphics, times(9)).blit(
                eq(texture),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyFloat(),
                anyFloat(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt()
        );
    }
}
