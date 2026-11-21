package dev.ClasherHD.bodycam.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PlatformCommonHelperTest {

    @AfterEach
    void cleanUp() {
        PlatformCommonHelper.clearAllForcedChunks(null);
    }

    @Test
    void forceChunk_neverUnforcesUntrackedChunks() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        PlatformCommonHelper.forceEntityChunk(level, UUID.randomUUID(), 4, -7, false);
        PlatformCommonHelper.forceBlockChunk(level, new BlockPos(64, 70, -112), 4, -7, false);

        verify(level, never()).setChunkForced(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void forceChunk_tracksMultipleOwnersPerChunkAndUnforcesOnlyWhenLastOwnerLeaves() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        UUID entityId = UUID.randomUUID();
        BlockPos blockPos = new BlockPos(16, 64, 32);

        PlatformCommonHelper.forceEntityChunk(level, entityId, 1, 2, true);
        PlatformCommonHelper.forceBlockChunk(level, blockPos, 1, 2, true);
        verify(level, times(1)).setChunkForced(1, 2, true);

        PlatformCommonHelper.forceEntityChunk(level, entityId, 1, 2, false);
        verify(level, never()).setChunkForced(1, 2, false);

        PlatformCommonHelper.forceBlockChunk(level, blockPos, 1, 2, false);
        verify(level, times(1)).setChunkForced(1, 2, false);

        // Repeated removal must not call setChunkForced(false) again
        PlatformCommonHelper.forceBlockChunk(level, blockPos, 1, 2, false);
        verify(level, times(1)).setChunkForced(1, 2, false);
    }

    @Test
    void clearEntityChunks_unforcesOnlyChunksOwnedByTargetEntity() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);

        UUID entityA = UUID.randomUUID();
        UUID entityB = UUID.randomUUID();

        PlatformCommonHelper.forceEntityChunk(level, entityA, 10, 20, true);
        PlatformCommonHelper.forceEntityChunk(level, entityA, 11, 20, true);
        PlatformCommonHelper.forceEntityChunk(level, entityB, 11, 20, true);

        PlatformCommonHelper.clearEntityChunks(server, entityA);

        verify(level, times(1)).setChunkForced(10, 20, false);
        verify(level, never()).setChunkForced(11, 20, false);
    }
}
