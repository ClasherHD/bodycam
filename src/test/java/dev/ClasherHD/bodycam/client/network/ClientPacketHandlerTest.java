package dev.ClasherHD.bodycam.client.network;

import dev.ClasherHD.bodycam.client.cache.ClientBodycamCache;
import dev.ClasherHD.bodycam.client.cache.ClientLocatorCache;
import dev.ClasherHD.bodycam.client.gui.BodycamViewScreen;
import dev.ClasherHD.bodycam.client.gui.DimensionLocatorScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerLocatorScreen;
import dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen;
import dev.ClasherHD.bodycam.client.gui.ServerConfigScreen;
import dev.ClasherHD.bodycam.client.gui.WatchPointRenameScreen;
import dev.ClasherHD.bodycam.network.bodycam.BodycamForceClosePacket;
import dev.ClasherHD.bodycam.network.bodycam.CrossObservationSyncPacket;
import dev.ClasherHD.bodycam.network.bodycam.SyncBodycamStatesS2CPacket;
import dev.ClasherHD.bodycam.network.config.OpenServerConfigS2CPacket;
import dev.ClasherHD.bodycam.network.locator.DimensionLocatorResponsePacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorStructureUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorSyncS2CPacket;
import dev.ClasherHD.bodycam.network.locator.PlayerLocatorTargetUpdateS2CPacket;
import dev.ClasherHD.bodycam.network.watchpoint.OpenWatchPointScreenS2CPacket;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
class ClientPacketHandlerTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @BeforeEach
    void resetCaches() {
        PlayerSelectionScreen.observingMe.clear();
        ClientBodycamCache.positions.clear();
        ClientBodycamCache.dimensions.clear();
        ClientLocatorCache.clear();
    }

    @Test
    @DisplayName("handleCrossSync adds and removes observer IDs in PlayerSelectionScreen.observingMe")
    void handleCrossSync_updatesObservingMe() {
        UUID observer = UUID.randomUUID();

        CrossObservationSyncPacket addMsg = new CrossObservationSyncPacket(observer, true);
        ClientPacketHandler.handleCrossSync(addMsg);
        assertThat(PlayerSelectionScreen.observingMe).contains(observer);

        CrossObservationSyncPacket removeMsg = new CrossObservationSyncPacket(observer, false);
        ClientPacketHandler.handleCrossSync(removeMsg);
        assertThat(PlayerSelectionScreen.observingMe).doesNotContain(observer);
    }

    @Test
    @DisplayName("handlePlayerLocatorStructureUpdate updates structure cache")
    void handlePlayerLocatorStructureUpdate_updatesCache() {
        BlockPos pos = new BlockPos(100, 64, -200);
        String dim = "minecraft:overworld";
        PlayerLocatorStructureUpdateS2CPacket msg = new PlayerLocatorStructureUpdateS2CPacket(pos, dim);

        ClientPacketHandler.handlePlayerLocatorStructureUpdate(msg);

        assertThat(ClientLocatorCache.structureTarget).isEqualTo(pos);
        assertThat(ClientLocatorCache.structureTargetDimension).isNotNull();
        assertThat(ClientLocatorCache.structureTargetDimension.location().toString()).isEqualTo(dim);
    }

    @Test
    @DisplayName("handlePlayerLocatorTargetUpdate updates target position and dimension")
    void handlePlayerLocatorTargetUpdate_updatesCache() {
        UUID target = UUID.randomUUID();
        BlockPos pos = new BlockPos(50, 70, 50);
        String dim = "minecraft:the_nether";
        PlayerLocatorTargetUpdateS2CPacket msg = new PlayerLocatorTargetUpdateS2CPacket(target, pos, dim);

        ClientPacketHandler.handlePlayerLocatorTargetUpdate(msg);

        assertThat(ClientBodycamCache.positions).containsEntry(target, pos);
        assertThat(ClientBodycamCache.dimensions).containsEntry(target, dim);
    }

    @Test
    @DisplayName("handleForceClose resets monitoring, resets camera entity to player, and closes screen")
    void handleForceClose_resetsStateAndScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            LocalPlayer player = mock(LocalPlayer.class);
            mc.player = player;
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            BodycamViewScreen.isMonitoring = true;
            ClientPacketHandler.handleForceClose(new BodycamForceClosePacket());

            assertThat(BodycamViewScreen.isMonitoring).isFalse();
            verify(mc).setCameraEntity(player);
            verify(mc).setScreen(null);
        }
    }

    @Test
    @DisplayName("handleDimensionResponse opens DimensionLocatorScreen")
    void handleDimensionResponse_opensScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            Map<UUID, String> dims = Map.of(UUID.randomUUID(), "minecraft:the_nether");
            ClientPacketHandler.handleDimensionResponse(new DimensionLocatorResponsePacket(dims));

            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(mc).setScreen(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(DimensionLocatorScreen.class);
        }
    }

    @Test
    @DisplayName("handleSyncStates updates cache and opens PlayerSelectionScreen")
    void handleSyncStates_updatesCacheAndOpensScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            UUID id = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            Map<UUID, Integer> jammers = Map.of(id, 1);
            Map<UUID, UUID> targets = Map.of(id, targetId);
            Map<UUID, String> dimensions = Map.of(id, "minecraft:overworld");
            Map<UUID, BlockPos> positions = Map.of(id, new BlockPos(1, 2, 3));
            Map<UUID, Boolean> anonymizers = Map.of(id, true);
            Set<UUID> hidden = Set.of(id);
            Map<UUID, String> watchPoints = Map.of(id, "Alpha");

            SyncBodycamStatesS2CPacket packet = new SyncBodycamStatesS2CPacket(
                    jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, 2, true
            );

            ClientPacketHandler.handleSyncStates(packet);

            assertThat(ClientBodycamCache.positions).containsEntry(id, new BlockPos(1, 2, 3));
            assertThat(ClientBodycamCache.dimensions).containsEntry(id, "minecraft:overworld");
            assertThat(ClientBodycamCache.jammers).containsEntry(id, 1);
            assertThat(ClientBodycamCache.anonymizers).containsEntry(id, true);
            assertThat(ClientBodycamCache.watchPoints).containsEntry(id, "Alpha");

            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(mc).setScreen(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(PlayerSelectionScreen.class);
        }
    }

    @Test
    @DisplayName("handleOpenServerConfig opens ServerConfigScreen")
    void handleOpenServerConfig_opensScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            OpenServerConfigS2CPacket packet = new OpenServerConfigS2CPacket(
                    100, 5, 20,
                    true, true, true, true, true, true, true, true, false
            );
            ClientPacketHandler.handleOpenServerConfig(packet);

            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(mc).setScreen(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(ServerConfigScreen.class);
        }
    }

    @Test
    @DisplayName("handleOpenWatchPointScreen opens WatchPointRenameScreen")
    void handleOpenWatchPointScreen_opensScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            OpenWatchPointScreenS2CPacket packet = new OpenWatchPointScreenS2CPacket(new BlockPos(5, 6, 7), "Base");
            ClientPacketHandler.handleOpenWatchPointScreen(packet);

            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(mc).setScreen(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(WatchPointRenameScreen.class);
        }
    }

    @Test
    @DisplayName("handlePlayerLocatorSync updates cache and opens PlayerLocatorScreen")
    void handlePlayerLocatorSync_updatesCacheAndOpensScreen() {
        try (MockedStatic<Minecraft> mockedMc = mockStatic(Minecraft.class)) {
            Minecraft mc = mock(Minecraft.class);
            mockedMc.when(Minecraft::getInstance).thenReturn(mc);

            UUID id = UUID.randomUUID();
            Map<UUID, Integer> jammers = Map.of(id, 2);
            Map<UUID, String> dimensions = Map.of(id, "minecraft:the_nether");
            Map<UUID, BlockPos> positions = Map.of(id, new BlockPos(100, 50, 100));

            PlayerLocatorSyncS2CPacket packet = new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, id, 1);
            ClientPacketHandler.handlePlayerLocatorSync(packet);

            assertThat(ClientBodycamCache.positions).containsEntry(id, new BlockPos(100, 50, 100));
            assertThat(ClientBodycamCache.dimensions).containsEntry(id, "minecraft:the_nether");

            ArgumentCaptor<Screen> captor = ArgumentCaptor.forClass(Screen.class);
            verify(mc).setScreen(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(PlayerLocatorScreen.class);
        }
    }
}
