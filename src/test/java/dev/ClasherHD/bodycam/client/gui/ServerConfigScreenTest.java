package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServerConfigScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("ServerConfigScreen constructor initializes boolean values from packet")
    void constructor_initializesFieldsFromPacket() throws Exception {
        OpenServerConfigS2CPacket packet = new OpenServerConfigS2CPacket(
                100, 5, 20,
                true, false, true,
                false, true, false,
                true, false, true,
                false, true, false,
                true, false, true
        );

        ServerConfigScreen screen = new ServerConfigScreen(null, packet);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.server_config.title"));

        Field reachField = ServerConfigScreen.class.getDeclaredField("reachVal");
        reachField.setAccessible(true);
        assertThat(reachField.getBoolean(screen)).isTrue();

        Field jammerField = ServerConfigScreen.class.getDeclaredField("jammerVal");
        jammerField.setAccessible(true);
        assertThat(jammerField.getBoolean(screen)).isFalse();

        Field locatorField = ServerConfigScreen.class.getDeclaredField("locatorVal");
        locatorField.setAccessible(true);
        assertThat(locatorField.getBoolean(screen)).isTrue();

        Field opOnlyField = ServerConfigScreen.class.getDeclaredField("opOnlyVal");
        opOnlyField.setAccessible(true);
        assertThat(opOnlyField.getBoolean(screen)).isTrue();

        Field loggerField = ServerConfigScreen.class.getDeclaredField("loggerVal");
        loggerField.setAccessible(true);
        assertThat(loggerField.getBoolean(screen)).isFalse();

        Field creepingDeathField = ServerConfigScreen.class.getDeclaredField("creepingDeathVal");
        creepingDeathField.setAccessible(true);
        assertThat(creepingDeathField.getBoolean(screen)).isTrue();
    }

    @Test
    @DisplayName("getToggleButtonMessage formats ON and OFF states correctly")
    void getToggleButtonMessage_formatsCorrectly() throws Exception {
        OpenServerConfigS2CPacket packet = new OpenServerConfigS2CPacket(
                100, 5, 20,
                true, true, true, true, true, true, true, true, false,
                true, true, true, true, true, true
        );
        ServerConfigScreen screen = new ServerConfigScreen(null, packet);

        Method method = ServerConfigScreen.class.getDeclaredMethod("getToggleButtonMessage", String.class, boolean.class);
        method.setAccessible(true);

        Component onMsg = (Component) method.invoke(screen, "gui.bodycam.server_config.enable_reach", true);
        assertThat(onMsg.getString()).contains("ON");

        Component offMsg = (Component) method.invoke(screen, "gui.bodycam.server_config.enable_reach", false);
        assertThat(offMsg.getString()).contains("OFF");
    }

    @Test
    @DisplayName("updateLayout and updateVisibility configure widgets for pages inside GUI window")
    void updateLayout_andVisibility_configuresWindowPages() throws Exception {
        OpenServerConfigS2CPacket packet = new OpenServerConfigS2CPacket(
                100, 5, 20,
                true, true, true, true, true, true, true, true, false,
                true, true, true, true, true, true
        );
        ServerConfigScreen screen = new ServerConfigScreen(null, packet);
        net.minecraft.client.gui.components.EditBox mockEditBox = mock(net.minecraft.client.gui.components.EditBox.class);
        net.minecraft.client.gui.components.Button mockButton = mock(net.minecraft.client.gui.components.Button.class);
        MinecraftTestEnvironment.setField(screen, "guiY", 30);
        MinecraftTestEnvironment.setField(screen, "txtMaxDistance", mockEditBox);
        MinecraftTestEnvironment.setField(screen, "btnReach", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnJammer", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnLocator", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnHologram", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnAnonymizer", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnPlayerLocator", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnLogger", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnPortablePortal", mockButton);
        MinecraftTestEnvironment.setField(screen, "txtMaxWatchPointsPlayer", mockEditBox);
        MinecraftTestEnvironment.setField(screen, "txtMaxWatchPointsTotal", mockEditBox);
        MinecraftTestEnvironment.setField(screen, "btnWatchPoint", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnPodest", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnAllSeeingEye", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnCreepingDeath", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnScholarCurse", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnCreepingDeathNuke", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnOpOnly", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnBack", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnNext", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnReset", mockButton);
        MinecraftTestEnvironment.setField(screen, "btnSave", mockButton);

        Method updateLayout = ServerConfigScreen.class.getDeclaredMethod("updateLayout");
        updateLayout.setAccessible(true);
        updateLayout.invoke(screen);

        Method updateVisibility = ServerConfigScreen.class.getDeclaredMethod("updateVisibility");
        updateVisibility.setAccessible(true);
        updateVisibility.invoke(screen);

        assertThat(screen.isPauseScreen()).isFalse();
        verify(mockEditBox, org.mockito.Mockito.atLeastOnce()).setY(64);
        verify(mockButton, org.mockito.Mockito.atLeastOnce()).setY(210);
    }

    @Test
    @DisplayName("onClose restores previous screen when minecraft instance is present")
    void onClose_restoresPreviousScreen() {
        OpenServerConfigS2CPacket packet = new OpenServerConfigS2CPacket(
                100, 5, 20,
                true, true, true, true, true, true, true, true, false,
                true, true, true, true, true, true
        );
        Screen previousScreen = mock(Screen.class);
        ServerConfigScreen screen = new ServerConfigScreen(previousScreen, packet);
        Minecraft mc = mock(Minecraft.class);
        MinecraftTestEnvironment.setField(screen, "minecraft", mc);

        screen.onClose();

        verify(mc).setScreen(previousScreen);
    }
}
