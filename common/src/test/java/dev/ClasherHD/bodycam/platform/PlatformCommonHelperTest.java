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
    void forceChunk_neverUnforcesUntrackedChunksAndRefCountsOwners() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        BlockPos blockOwner = new BlockPos(16, 64, -32);

        // Unforcing an untracked chunk must never disturb vanilla /forceload or other mods
        PlatformCommonHelper.forceEntityChunk(level, ownerA, 5, -3, false);
        PlatformCommonHelper.forceBlockChunk(level, blockOwner, 5, -3, false);
        verify(level, never()).setChunkForced(anyInt(), anyInt(), anyBoolean());

        // First owner forces the chunk once
        PlatformCommonHelper.forceEntityChunk(level, ownerA, 5, -3, true);
        verify(level, times(1)).setChunkForced(5, -3, true);

        // Second owner shares the chunk without redundant force calls
        PlatformCommonHelper.forceBlockChunk(level, blockOwner, 5, -3, true);
        verify(level, times(1)).setChunkForced(5, -3, true);

        // Removing first owner keeps chunk forced for second owner
        PlatformCommonHelper.forceEntityChunk(level, ownerA, 5, -3, false);
        verify(level, never()).setChunkForced(5, -3, false);

        // Removing last owner unforces chunk once
        PlatformCommonHelper.forceBlockChunk(level, blockOwner, 5, -3, false);
        verify(level, times(1)).setChunkForced(5, -3, false);

        // Repeated removal must not unforce again
        PlatformCommonHelper.forceBlockChunk(level, blockOwner, 5, -3, false);
        verify(level, times(1)).setChunkForced(5, -3, false);
    }

    @Test
    void clearEntityChunks_unforcesOnlyTrackedChunksOfThatEntity() {
        ServerLevel level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);

        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();

        PlatformCommonHelper.forceEntityChunk(level, ownerA, 1, 2, true);
        PlatformCommonHelper.forceEntityChunk(level, ownerA, 3, 4, true);
        PlatformCommonHelper.forceEntityChunk(level, ownerB, 3, 4, true);

        PlatformCommonHelper.clearEntityChunks(server, ownerA);

        verify(level, times(1)).setChunkForced(1, 2, false);
        verify(level, never()).setChunkForced(3, 4, false);

        PlatformCommonHelper.clearEntityChunks(server, ownerB);
        verify(level, times(1)).setChunkForced(3, 4, false);
    }
}
