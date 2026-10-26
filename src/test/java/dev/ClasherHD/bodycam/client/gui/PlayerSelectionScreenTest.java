package dev.ClasherHD.bodycam.client.gui;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.config.ModServerConfig;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PlayerSelectionScreenTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
        com.electronwill.nightconfig.core.CommentedConfig serverConfig = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        ModServerConfig.SPEC.acceptConfig(serverConfig);
    }

    @BeforeEach
    void resetState() {
        PlayerSelectionScreen.observingMe.clear();
        ClientBodycamCache.positions.clear();
        ClientBodycamCache.dimensions.clear();
    }

    @Test
    @DisplayName("PlayerSelectionScreen constructor sets reachLevel, isOnHologram, and title")
    void constructor_setsFieldsCorrectly() throws Exception {
        PlayerSelectionScreen screen = new PlayerSelectionScreen(1, true);

        assertThat(screen.getTitle()).isEqualTo(Component.translatable("gui.bodycam.select_player"));
        assertThat(screen.isPauseScreen()).isFalse();

        Field reachField = PlayerSelectionScreen.class.getDeclaredField("reachLevel");
        reachField.setAccessible(true);
        assertThat(reachField.getInt(screen)).isEqualTo(1);

        Field holoField = PlayerSelectionScreen.class.getDeclaredField("isOnHologram");
        holoField.setAccessible(true);
        assertThat(holoField.getBoolean(screen)).isTrue();
    }

    @Test
    @DisplayName("observingMe set tracks observing player UUIDs thread-safely")
    void observingMe_setOperations() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PlayerSelectionScreen.observingMe.add(id1);
        assertThat(PlayerSelectionScreen.observingMe).contains(id1).doesNotContain(id2);

        PlayerSelectionScreen.observingMe.add(id2);
        assertThat(PlayerSelectionScreen.observingMe).contains(id1, id2);

        PlayerSelectionScreen.observingMe.remove(id1);
        assertThat(PlayerSelectionScreen.observingMe).doesNotContain(id1).contains(id2);
    }

    @Test
    @DisplayName("isBeyondReach returns false when reachLevel is 2 or higher")
    void isBeyondReach_withInfiniteReach() throws Exception {
        PlayerSelectionScreen screen = new PlayerSelectionScreen(2, false);
        Method method = PlayerSelectionScreen.class.getDeclaredMethod("isBeyondReach", UUID.class);
        method.setAccessible(true);

        boolean beyond = (boolean) method.invoke(screen, UUID.randomUUID());
        assertThat(beyond).isFalse();
    }

    @Test
    @DisplayName("isBeyondReach evaluates correctly with mocked Minecraft client and positions")
    void isBeyondReach_evaluatesScenarios() throws Exception {
        UUID targetId = UUID.randomUUID();
        Method method = PlayerSelectionScreen.class.getDeclaredMethod("isBeyondReach", UUID.class);
        method.setAccessible(true);

        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            LocalPlayer player = mock(LocalPlayer.class);
            Level level = mock(Level.class);
            ResourceKey<Level> overworldKey = Level.OVERWORLD;

            when(level.dimension()).thenReturn(overworldKey);
            when(player.level()).thenReturn(level);
            when(player.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
            mc.player = player;
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            PlayerSelectionScreen screenReach0 = new PlayerSelectionScreen(0, false);
            boolean missingTarget = (boolean) method.invoke(screenReach0, targetId);
            assertThat(missingTarget).isTrue();

            ClientBodycamCache.positions.put(targetId, new BlockPos(100, 64, 0));
            ClientBodycamCache.dimensions.put(targetId, "minecraft:the_nether");
            boolean diffDimension = (boolean) method.invoke(screenReach0, targetId);
            assertThat(diffDimension).isTrue();

            ClientBodycamCache.dimensions.put(targetId, "minecraft:overworld");
            boolean inRangeReach0 = (boolean) method.invoke(screenReach0, targetId);
            assertThat(inRangeReach0).isFalse();

            ClientBodycamCache.positions.put(targetId, new BlockPos(10000, 64, 0));
            boolean outOfRangeReach0 = (boolean) method.invoke(screenReach0, targetId);
            assertThat(outOfRangeReach0).isTrue();

            PlayerSelectionScreen screenReach1 = new PlayerSelectionScreen(1, false);
            boolean inDimensionReach1 = (boolean) method.invoke(screenReach1, targetId);
            assertThat(inDimensionReach1).isFalse();
        }
    }

    @Test
    @DisplayName("isOtherDimensionWithReach returns true only when reachLevel is 1 and dimensions differ")
    void isOtherDimensionWithReach_evaluatesCorrectly() throws Exception {
        UUID targetId = UUID.randomUUID();
        Method method = PlayerSelectionScreen.class.getDeclaredMethod("isOtherDimensionWithReach", UUID.class);
        method.setAccessible(true);

        PlayerSelectionScreen screenLevel0 = new PlayerSelectionScreen(0, false);
        PlayerSelectionScreen screenLevel1 = new PlayerSelectionScreen(1, false);
        PlayerSelectionScreen screenLevel2 = new PlayerSelectionScreen(2, false);

        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            LocalPlayer player = mock(LocalPlayer.class);
            Level level = mock(Level.class);
            when(level.dimension()).thenReturn(Level.OVERWORLD);
            when(player.level()).thenReturn(level);
            mc.player = player;
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            assertThat((boolean) method.invoke(screenLevel0, targetId)).isFalse();
            assertThat((boolean) method.invoke(screenLevel2, targetId)).isFalse();

            ClientBodycamCache.dimensions.put(targetId, "minecraft:overworld");
            assertThat((boolean) method.invoke(screenLevel1, targetId)).isFalse();

            ClientBodycamCache.dimensions.put(targetId, "minecraft:the_nether");
            assertThat((boolean) method.invoke(screenLevel1, targetId)).isTrue();
        }
    }

    @Test
    @DisplayName("WINDOW_TEXTURE has correct ResourceLocation")
    void windowTexture_hasExpectedPath() throws Exception {
        Field texField = PlayerSelectionScreen.class.getDeclaredField("WINDOW_TEXTURE");
        texField.setAccessible(true);
        ResourceLocation loc = (ResourceLocation) texField.get(null);
        assertThat(loc).isEqualTo(new ResourceLocation("bodycam", "textures/gui/gui_window.png"));
    }
}
