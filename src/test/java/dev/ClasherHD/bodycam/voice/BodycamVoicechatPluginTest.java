package dev.ClasherHD.bodycam.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.lwjgl.openal.AL10;

class BodycamVoicechatPluginTest {

    @Test
    @DisplayName("Plugin ID should be bodycam")
    void getPluginId_returnsBodycam() {
        BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
        assertThat(plugin.getPluginId()).isEqualTo("bodycam");
    }

    @Test
    @DisplayName("registerEvents does not throw exception")
    void registerEvents_completesNormally() {
        BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
        assertThatCode(() -> plugin.registerEvents(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("initialize stores VoicechatApi reference and registers to event bus")
    void initialize_storesApiReference() {
        BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
        VoicechatApi dummyApi = (VoicechatApi) java.lang.reflect.Proxy.newProxyInstance(
                VoicechatApi.class.getClassLoader(),
                new Class<?>[]{VoicechatApi.class},
                (proxy, method, args) -> null
        );

        plugin.initialize(dummyApi);
        assertThat(BodycamVoicechatPlugin.voicechatApi).isSameAs(dummyApi);

        MinecraftForge.EVENT_BUS.unregister(plugin);
    }

    @Test
    @DisplayName("onClientTick ignores non-END phase")
    void onClientTick_ignoresStartPhase() {
        BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
        TickEvent.ClientTickEvent startEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.START);
        assertThatCode(() -> plugin.onClientTick(startEvent)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onClientTick END phase throws NPE when Minecraft instance is null")
    void onClientTick_endPhase_throwsWhenMinecraftNull() {
        BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
        TickEvent.ClientTickEvent endEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
        assertThatThrownBy(() -> plugin.onClientTick(endEvent))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("onClientTick END phase does nothing when screen is not BodycamViewScreen")
    void onClientTick_endPhase_noViewScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mc.screen = null;
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
            TickEvent.ClientTickEvent endEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
            assertThatCode(() -> plugin.onClientTick(endEvent)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("onClientTick END phase updates listener position safely when viewing alive player")
    void onClientTick_endPhase_withTargetPlayer() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class);
             MockedStatic<AL10> mockedAl = mockStatic(AL10.class)) {
            Minecraft mc = mock(Minecraft.class);
            BodycamViewScreen screen = MinecraftTestEnvironment.allocateInstance(BodycamViewScreen.class);
            UUID targetId = UUID.randomUUID();
            MinecraftTestEnvironment.setField(screen, "targetId", targetId);
            mc.screen = screen;

            ClientLevel level = mock(ClientLevel.class);
            Player player = mock(Player.class);
            when(player.isAlive()).thenReturn(true);
            when(player.getX()).thenReturn(10.0D);
            when(player.getEyeY()).thenReturn(65.5D);
            when(player.getZ()).thenReturn(-20.0D);
            when(level.getPlayerByUUID(targetId)).thenReturn(player);
            mc.level = level;

            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
            TickEvent.ClientTickEvent endEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
            plugin.onClientTick(endEvent);

            mockedAl.verify(() -> AL10.alListener3f(AL10.AL_POSITION, 10.0f, 65.5f, -20.0f));
        }
    }

    @Test
    @DisplayName("onClientTick END phase ignores dead or null player targets")
    void onClientTick_endPhase_deadOrMissingPlayer() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            BodycamViewScreen screen = MinecraftTestEnvironment.allocateInstance(BodycamViewScreen.class);
            UUID targetId = UUID.randomUUID();
            MinecraftTestEnvironment.setField(screen, "targetId", targetId);
            mc.screen = screen;

            ClientLevel level = mock(ClientLevel.class);
            Player deadPlayer = mock(Player.class);
            when(deadPlayer.isAlive()).thenReturn(false);
            when(level.getPlayerByUUID(targetId)).thenReturn(deadPlayer);
            mc.level = level;

            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            BodycamVoicechatPlugin plugin = new BodycamVoicechatPlugin();
            TickEvent.ClientTickEvent endEvent = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
            assertThatCode(() -> plugin.onClientTick(endEvent)).doesNotThrowAnyException();
        }
    }
}
