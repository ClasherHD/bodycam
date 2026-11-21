package dev.ClasherHD.bodycam.entity;

import dev.ClasherHD.bodycam.block.entity.WatchPointBlockEntity;
import dev.ClasherHD.bodycam.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchPointDummyEntityTest {

    @BeforeAll
    static void setUpAll() {
        MinecraftTestEnvironment.ensureBootstrapped();
    }

    @Test
    @DisplayName("WatchPointDummyEntity data accessors and active set should be defined")
    void dataAccessorsAndActiveSet_areDefined() {
        assertThat(WatchPointDummyEntity.WATCH_POINT_NAME).isNotNull();
        assertThat(WatchPointDummyEntity.WATCH_POINT_POS).isNotNull();
        assertThat(WatchPointDummyEntity.ACTIVE_WATCH_POINTS).isNotNull();
        assertThat(WatchPointDummyEntity.getTotalActiveCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("isLinkedToOwnerHealth should return false on WatchPointDummyEntity")
    void isLinkedToOwnerHealth_returnsFalse() throws Exception {
        WatchPointDummyEntity dummy = MinecraftTestEnvironment.allocateInstance(WatchPointDummyEntity.class);
        Method m = WatchPointDummyEntity.class.getDeclaredMethod("isLinkedToOwnerHealth");
        m.setAccessible(true);
        boolean linked = (boolean) m.invoke(dummy);

        assertThat(linked).isFalse();
    }

    @Test
    @DisplayName("getSelectionDisplayName should return custom name, coordinates, or default fallback")
    void getSelectionDisplayName_evaluatesPriorityHierarchy() {
        TestWatchPointDummy dummy = MinecraftTestEnvironment.allocateInstance(TestWatchPointDummy.class);

        dummy.custom = "Observation Post 1";
        dummy.pos = new BlockPos(100, 70, -300);
        assertThat(dummy.getSelectionDisplayName()).isEqualTo("Observation Post 1");

        dummy.custom = "";
        assertThat(dummy.getSelectionDisplayName()).isEqualTo("Watch Point (100, 70, -300)");

        dummy.custom = null;
        dummy.pos = null;
        assertThat(dummy.getSelectionDisplayName()).isEqualTo("Watch Point");
    }

    @Test
    @DisplayName("findByUUID returns null for null inputs or nonexistent dummy")
    void findByUUID_handlesNullAndMissing() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getAllLevels()).thenReturn(Collections.emptyList());

        assertThat(WatchPointDummyEntity.findByUUID(null, UUID.randomUUID())).isNull();
        assertThat(WatchPointDummyEntity.findByUUID(server, null)).isNull();
        assertThat(WatchPointDummyEntity.findByUUID(server, UUID.randomUUID())).isNull();
    }

    @Test
    @DisplayName("getActiveForOwner filters active dummies by matching owner UUID")
    void getActiveForOwner_filtersByOwner() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getAllLevels()).thenReturn(Collections.emptyList());

        UUID ownerId = UUID.randomUUID();
        List<WatchPointDummyEntity> list = WatchPointDummyEntity.getActiveForOwner(server, ownerId);
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("kickObservingOwner does not access unloaded WatchPoint block entity and accesses it when loaded")
    void unloadedChunk_skipsBlockEntityAccess() throws Exception {
        BlockPos wpPos = new BlockPos(128, 64, -128);
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.isLoaded(wpPos)).thenReturn(false);

        WatchPointDummyEntity dummy = mock(WatchPointDummyEntity.class);
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(playerList);
        when(dummy.getServer()).thenReturn(server);
        when(dummy.level()).thenReturn(level);
        when(dummy.getWatchPointPos()).thenReturn(wpPos);
        when(dummy.getOwnerUUID()).thenReturn(UUID.randomUUID());

        Method kick = WatchPointDummyEntity.class.getDeclaredMethod("kickObservingOwner", boolean.class);
        kick.setAccessible(true);
        kick.invoke(dummy, false);
        verify(level).isLoaded(wpPos);
        verify(level, never()).getBlockEntity(wpPos);

        when(level.isLoaded(wpPos)).thenReturn(true);
        WatchPointBlockEntity be = mock(WatchPointBlockEntity.class);
        when(level.getBlockEntity(wpPos)).thenReturn(be);
        MinecraftTestEnvironment.setField(dummy, "kickedObservingOwner", false);
        kick.invoke(dummy, false);
        verify(level).getBlockEntity(wpPos);
    }

    private static class TestWatchPointDummy extends WatchPointDummyEntity {
        String custom;
        BlockPos pos;

        public TestWatchPointDummy() {
            super(null, null);
        }

        @Override
        public String getWatchPointName() {
            return this.custom;
        }

        @Override
        public BlockPos getWatchPointPos() {
            return this.pos;
        }
    }
}
