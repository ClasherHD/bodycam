package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class WatchPointRenameScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("WatchPointRenameScreen constructor sets pos and initialName")
    void constructor_setsFieldsCorrectly() throws Exception {
        BlockPos pos = new BlockPos(12, 64, -34);
        String name = "Outpost Alpha";

        WatchPointRenameScreen screen = new WatchPointRenameScreen(pos, name);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.watch_point.rename_title"));
        assertThat(screen.isPauseScreen()).isFalse();

        Field posField = WatchPointRenameScreen.class.getDeclaredField("pos");
        posField.setAccessible(true);
        assertThat(posField.get(screen)).isEqualTo(pos);

        Field nameField = WatchPointRenameScreen.class.getDeclaredField("initialName");
        nameField.setAccessible(true);
        assertThat(nameField.get(screen)).isEqualTo(name);
    }

    @Test
    @DisplayName("WatchPointRenameScreen defaults null initialName to empty string")
    void constructor_nullNameDefaultsToEmptyString() throws Exception {
        WatchPointRenameScreen screen = new WatchPointRenameScreen(BlockPos.ZERO, null);

        Field nameField = WatchPointRenameScreen.class.getDeclaredField("initialName");
        nameField.setAccessible(true);
        assertThat(nameField.get(screen)).isEqualTo("");
    }

    @Test
    @DisplayName("WatchPointRenameScreen defines standard window texture and dimensions")
    void windowTextureAndDimensions_haveExpectedValues() throws Exception {
        Field texField = WatchPointRenameScreen.class.getDeclaredField("WINDOW_TEXTURE");
        texField.setAccessible(true);
        ResourceLocation texture = (ResourceLocation) texField.get(null);
        assertThat(texture).isEqualTo(new ResourceLocation("bodycam", "textures/gui/gui_window.png"));

        WatchPointRenameScreen screen = new WatchPointRenameScreen(BlockPos.ZERO, "Test");
        Field widthField = WatchPointRenameScreen.class.getDeclaredField("guiWidth");
        widthField.setAccessible(true);
        assertThat(widthField.getInt(screen)).isEqualTo(220);

        Field heightField = WatchPointRenameScreen.class.getDeclaredField("guiHeight");
        heightField.setAccessible(true);
        assertThat(heightField.getInt(screen)).isEqualTo(100);
    }

    @Test
    @DisplayName("keyPressed delegates to super on arbitrary keys")
    void keyPressed_arbitraryKey_returnsFalse() {
        WatchPointRenameScreen screen = new WatchPointRenameScreen(BlockPos.ZERO, "Test");
        boolean handled = screen.keyPressed(GLFW.GLFW_KEY_A, 0, 0);
        assertThat(handled).isFalse();
    }
}
