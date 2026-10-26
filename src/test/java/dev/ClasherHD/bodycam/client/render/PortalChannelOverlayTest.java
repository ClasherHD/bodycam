package dev.ClasherHD.bodycam.client.render;

import dev.ClasherHD.bodycam.registry.ModItems;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PortalChannelOverlayTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("PortalChannelOverlay singleton INSTANCE is initialized")
    void instance_isNotNull() {
        assertThat(PortalChannelOverlay.INSTANCE).isNotNull();
    }

    @Test
    @DisplayName("PortalChannelSoundInstance initializes properties and stopSound marks it stopped")
    void soundInstance_propertiesAndStop() {
        ServerPlayer player = MinecraftTestEnvironment.allocateInstance(ServerPlayer.class);
        MinecraftTestEnvironment.setField(player, "random", net.minecraft.util.RandomSource.create());

        PortalChannelOverlay.PortalChannelSoundInstance sound =
                new PortalChannelOverlay.PortalChannelSoundInstance(player);

        assertThat(sound.getLocation()).isEqualTo(SoundEvents.PORTAL_TRIGGER.getLocation());
        assertThat(sound.getSource()).isEqualTo(SoundSource.AMBIENT);
        float volume = MinecraftTestEnvironment.getField(sound, "volume");
        float pitch = MinecraftTestEnvironment.getField(sound, "pitch");
        assertThat(volume).isEqualTo(0.25F);
        assertThat(pitch).isBetween(0.8F, 1.2F);
        assertThat(sound.isStopped()).isFalse();

        sound.stopSound();
        assertThat(sound.isStopped()).isTrue();
    }

    @Test
    @DisplayName("PortalChannelSoundInstance tick stops sound when player is removed or stops using portal")
    void soundInstance_tickConditions() {
        Player player = mock(Player.class);
        when(player.getRandom()).thenReturn(net.minecraft.util.RandomSource.create());

        PortalChannelOverlay.PortalChannelSoundInstance sound =
                new PortalChannelOverlay.PortalChannelSoundInstance(player);

        when(player.isRemoved()).thenReturn(true);
        sound.tick();
        assertThat(sound.isStopped()).isTrue();

        sound = new PortalChannelOverlay.PortalChannelSoundInstance(player);
        when(player.isRemoved()).thenReturn(false);
        when(player.isUsingItem()).thenReturn(false);
        sound.tick();
        assertThat(sound.isStopped()).isTrue();

        sound = new PortalChannelOverlay.PortalChannelSoundInstance(player);
        when(player.isRemoved()).thenReturn(false);
        when(player.isUsingItem()).thenReturn(true);
        when(player.getUseItem()).thenReturn(new ItemStack(Items.APPLE));
        sound.tick();
        assertThat(sound.isStopped()).isTrue();

        sound = new PortalChannelOverlay.PortalChannelSoundInstance(player);
        when(player.isRemoved()).thenReturn(false);
        when(player.isUsingItem()).thenReturn(true);
        when(player.getUseItem()).thenReturn(new ItemStack(ModItems.PORTABLE_NETHER_PORTAL.get()));
        sound.tick();
        assertThat(sound.isStopped()).isFalse();
    }

    @Test
    @DisplayName("render stops sound when player is null or not using item")
    void render_playerNullOrNotUsing_stopsActiveSound() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            SoundManager soundManager = mock(SoundManager.class);
            when(mc.getSoundManager()).thenReturn(soundManager);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            Player player = mock(Player.class);
            when(player.getRandom()).thenReturn(net.minecraft.util.RandomSource.create());
            PortalChannelOverlay.PortalChannelSoundInstance activeSound =
                    new PortalChannelOverlay.PortalChannelSoundInstance(player);

            MinecraftTestEnvironment.setField(PortalChannelOverlay.INSTANCE, "activeSound", activeSound);

            mc.player = null;
            PortalChannelOverlay.INSTANCE.render(null, null, 0.0F, 1920, 1080);

            assertThat(activeSound.isStopped()).isTrue();
            verify(soundManager).stop(activeSound);
            PortalChannelOverlay.PortalChannelSoundInstance stored =
                    MinecraftTestEnvironment.getField(PortalChannelOverlay.INSTANCE, "activeSound");
            assertThat(stored).isNull();
        }
    }

    @Test
    @DisplayName("stopSound safely handles null activeSound")
    void stopSound_withNullActiveSound_doesNothing() throws Exception {
        Minecraft mc = mock(Minecraft.class);
        MinecraftTestEnvironment.setField(PortalChannelOverlay.INSTANCE, "activeSound", null);

        Method stopSound = PortalChannelOverlay.class.getDeclaredMethod("stopSound", Minecraft.class);
        stopSound.setAccessible(true);

        assertThatCode(() -> stopSound.invoke(PortalChannelOverlay.INSTANCE, mc))
                .doesNotThrowAnyException();
    }
}
