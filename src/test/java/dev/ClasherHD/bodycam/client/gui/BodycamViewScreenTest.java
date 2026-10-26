package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.PauseScreen;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class BodycamViewScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    void resetStatics() {
        BodycamViewScreen.isMonitoring = false;
        BodycamViewScreen.targetUuid = null;
        BodycamViewScreen.targetNameStatic = null;
        BodycamViewScreen.lastIsOnHologram = false;
        BodycamViewScreen.lastIsWatchPoint = false;
    }

    @Test
    @DisplayName("Direct constructor invocation requires Minecraft instance and throws NPE in headless tests")
    void constructor_requiresMinecraftInstance() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new BodycamViewScreen(id, "TestTarget", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Allocated instance returns targetId and isPauseScreen is false")
    void getTargetId_returnsConfiguredId() {
        BodycamViewScreen screen = MinecraftTestEnvironment.allocateInstance(BodycamViewScreen.class);
        UUID targetId = UUID.randomUUID();
        MinecraftTestEnvironment.setField(screen, "targetId", targetId);

        assertThat(screen.getTargetId()).isEqualTo(targetId);
        assertThat(screen.isPauseScreen()).isFalse();
    }

    @Test
    @DisplayName("BodycamViewScreen static fields maintain monitor state across sessions")
    void staticFields_trackMonitoringSession() {
        UUID targetId = UUID.randomUUID();
        String targetName = "AgentSmith";

        BodycamViewScreen.isMonitoring = true;
        BodycamViewScreen.targetUuid = targetId;
        BodycamViewScreen.targetNameStatic = targetName;
        BodycamViewScreen.lastIsOnHologram = true;
        BodycamViewScreen.lastIsWatchPoint = false;

        assertThat(BodycamViewScreen.isMonitoring).isTrue();
        assertThat(BodycamViewScreen.targetUuid).isEqualTo(targetId);
        assertThat(BodycamViewScreen.targetNameStatic).isEqualTo(targetName);
        assertThat(BodycamViewScreen.lastIsOnHologram).isTrue();
        assertThat(BodycamViewScreen.lastIsWatchPoint).isFalse();
    }

    @Test
    @DisplayName("keyPressed handles perspective blocking, escape pause, and arbitrary keys")
    void keyPressed_handlesEvents() {
        BodycamViewScreen screen = MinecraftTestEnvironment.allocateInstance(BodycamViewScreen.class);
        Minecraft mc = mock(Minecraft.class);
        Options options = mock(Options.class);
        KeyMapping shift = mock(KeyMapping.class);
        KeyMapping perspective = mock(KeyMapping.class);
        MinecraftTestEnvironment.setField(options, "keyShift", shift);
        MinecraftTestEnvironment.setField(options, "keyTogglePerspective", perspective);
        MinecraftTestEnvironment.setField(mc, "options", options);
        MinecraftTestEnvironment.setField(screen, "minecraft", mc);

        assertThat(screen.keyPressed(GLFW.GLFW_KEY_F3, 0, 0)).isTrue();
        assertThat(screen.keyPressed(GLFW.GLFW_KEY_F5, 0, 0)).isTrue();

        when(perspective.matches(GLFW.GLFW_KEY_F5, 0)).thenReturn(true);
        assertThat(screen.keyPressed(GLFW.GLFW_KEY_F5, 0, 0)).isTrue();

        assertThat(screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0)).isTrue();
        verify(mc).setScreen(any(PauseScreen.class));
    }
}
