package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClientConfigScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        com.electronwill.nightconfig.core.CommentedConfig config = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        dev.ClasherHD.bodycam.config.ModClientConfig.SPEC.acceptConfig(config);
    }

    @Test
    @DisplayName("ClientConfigScreen constructor initializes values from ModClientConfig")
    void constructor_initializesFields() throws Exception {
        ClientConfigScreen screen = new ClientConfigScreen(null);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.config.title"));

        Field prevField = ClientConfigScreen.class.getDeclaredField("previous");
        prevField.setAccessible(true);
        assertThat(prevField.get(screen)).isNull();

        Field nameValField = ClientConfigScreen.class.getDeclaredField("nameVal");
        nameValField.setAccessible(true);
        assertThat(nameValField.getBoolean(screen)).isTrue();
    }

    @Test
    @DisplayName("getClosestPresetIndex maps exact colors to correct presets")
    void getClosestPresetIndex_exactMatches() throws Exception {
        ClientConfigScreen screen = new ClientConfigScreen(null);
        Method method = ClientConfigScreen.class.getDeclaredMethod("getClosestPresetIndex", String.class);
        method.setAccessible(true);

        int whiteIdx = (int) method.invoke(screen, "FFFFFF");
        assertThat(whiteIdx).isEqualTo(0);

        int blackIdx = (int) method.invoke(screen, "000000");
        assertThat(blackIdx).isEqualTo(3);

        int fallbackIdx = (int) method.invoke(screen, "NOT_A_COLOR");
        assertThat(fallbackIdx).isEqualTo(0);
    }

    @Test
    @DisplayName("getToggleButtonMessage formats ON and OFF states correctly")
    void getToggleButtonMessage_formatsCorrectly() throws Exception {
        ClientConfigScreen screen = new ClientConfigScreen(null);
        Method method = ClientConfigScreen.class.getDeclaredMethod("getToggleButtonMessage", String.class, boolean.class);
        method.setAccessible(true);

        Component onMsg = (Component) method.invoke(screen, "gui.bodycam.config.show_name", true);
        assertThat(onMsg.getString()).contains("ON");

        Component offMsg = (Component) method.invoke(screen, "gui.bodycam.config.show_name", false);
        assertThat(offMsg.getString()).contains("OFF");
    }

    @Test
    @DisplayName("getColorButtonMessage formats color label and translation correctly")
    void getColorButtonMessage_formatsCorrectly() throws Exception {
        ClientConfigScreen screen = new ClientConfigScreen(null);
        Method method = ClientConfigScreen.class.getDeclaredMethod("getColorButtonMessage", String.class, int.class);
        method.setAccessible(true);

        Component msg = (Component) method.invoke(screen, "gui.bodycam.config.color_standard", 0);
        assertThat(msg.getString()).contains("color.bodycam.white");
    }

    @Test
    @DisplayName("mouseScrolled adjusts and clamps scrollY within valid range")
    void mouseScrolled_clampsScrollRange() throws Exception {
        ClientConfigScreen screen = new ClientConfigScreen(null);
        net.minecraft.client.gui.components.Button mockButton = mock(net.minecraft.client.gui.components.Button.class);
        MinecraftTestEnvironment.setField(screen, "btnName", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnHealth", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnShift", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorStd", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorBlk", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorObs", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorDim", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorOor", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnColorOtd", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnReset", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnDone", mockButton);

        Field scrollField = ClientConfigScreen.class.getDeclaredField("scrollY");
        scrollField.setAccessible(true);

        screen.mouseScrolled(0, 0, 5.0D);
        assertThat(scrollField.getDouble(screen)).isEqualTo(0.0D);

        screen.mouseScrolled(0, 0, -10.0D);
        assertThat(scrollField.getDouble(screen)).isEqualTo(-70.0D);
    }

    @Test
    @DisplayName("onClose restores previous screen when minecraft instance is present")
    void onClose_restoresPreviousScreen() {
        Screen previousScreen = mock(Screen.class);
        ClientConfigScreen screen = new ClientConfigScreen(previousScreen);
        Minecraft mc = mock(Minecraft.class);
        MinecraftTestEnvironment.setField(screen, "minecraft", mc);

        screen.onClose();

        verify(mc).setScreen(previousScreen);
    }
}
