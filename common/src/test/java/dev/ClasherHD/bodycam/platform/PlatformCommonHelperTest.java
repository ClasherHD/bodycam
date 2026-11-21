package dev.ClasherHD.bodycam.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PlatformCommonHelperTest {

    @AfterEach
    void tearDown() {
        PlatformCommonHelper.clearAllForcedChunks(null);
    }

    @Test
    void forceEntityChunk_ignoresUntrackedUnforceAndRefCountsOwners() {
        ServerLevel level = Mockito.mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        // Untracked unforce must not unforce an external /forceload chunk.
        PlatformCommonHelper.forceEntityChunk(level, owner1, 5, -3, false);
        verify(level, never()).setChunkForced(5, -3, false);

        PlatformCommonHelper.forceEntityChunk(level, owner1, 5, -3, true);
        PlatformCommonHelper.forceEntityChunk(level, owner2, 5, -3, true);
        verify(level, times(1)).setChunkForced(5, -3, true);

        PlatformCommonHelper.forceEntityChunk(level, owner1, 5, -3, false);
        verify(level, never()).setChunkForced(5, -3, false);

        PlatformCommonHelper.forceEntityChunk(level, owner2, 5, -3, false);
        verify(level, times(1)).setChunkForced(5, -3, false);
    }

    @Test
    void clearEntityChunks_unforcesOnlyWhenLastOwnerRemoved() {
        ServerLevel level = Mockito.mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        MinecraftServer server = Mockito.mock(MinecraftServer.class);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);

        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        PlatformCommonHelper.forceEntityChunk(level, owner1, 1, 2, true);
        PlatformCommonHelper.forceEntityChunk(level, owner2, 1, 2, true);

        PlatformCommonHelper.clearEntityChunks(server, owner1);
        verify(level, never()).setChunkForced(1, 2, false);

        PlatformCommonHelper.clearEntityChunks(server, owner2);
        verify(level, times(1)).setChunkForced(1, 2, false);
    }
}
