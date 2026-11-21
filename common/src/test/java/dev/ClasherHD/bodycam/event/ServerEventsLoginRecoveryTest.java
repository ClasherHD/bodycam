package dev.ClasherHD.bodycam.event;

import dev.ClasherHD.bodycam.network.bodycam.BodycamResetCameraPacket;
import dev.ClasherHD.bodycam.platform.PlatformCommonHelper;
import dev.ClasherHD.bodycam.util.BodycamPersistentDataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("null")
class ServerEventsLoginRecoveryTest {

    @Test
    void onPlayerLogin_handlesCorruptedUuidAndNanCoordinatesWithoutThrowingOrTeleportingToNan() {
        UUID playerId = UUID.randomUUID();
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        ServerLevel overworld = mock(ServerLevel.class);
        when(server.getPlayerList()).thenReturn(playerList);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(overworld);
        when(server.overworld()).thenReturn(overworld);
        when(server.getAllLevels()).thenReturn(java.util.List.of(overworld));
        when(overworld.getSharedSpawnPos()).thenReturn(new BlockPos(0, 64, 0));

        ServerPlayer player = mock(ServerPlayer.class, withSettings().extraInterfaces(BodycamPersistentDataHolder.class));
        try {
            var serverField = ServerPlayer.class.getField("server");
            serverField.setAccessible(true);
            serverField.set(player, server);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        when(player.getUUID()).thenReturn(playerId);
        when(player.level()).thenReturn(overworld);
        when(overworld.isClientSide()).thenReturn(false);

        CompoundTag data = new CompoundTag();
        data.putBoolean("bodycam_active", true);
        data.putString("bodycam_orig_dim", "minecraft:overworld");
        data.putDouble("bodycam_orig_x", Double.NaN);
        data.putDouble("bodycam_orig_y", 64.0D);
        data.putDouble("bodycam_orig_z", 0.0D);
        data.putFloat("bodycam_orig_yrot", 0.0F);
        data.putFloat("bodycam_orig_xrot", 0.0F);
        // Corrupted non-UUID tag types (would crash getUUID without hasUUID guard)
        data.putString("bodycam_dummy_uuid", "not-a-4-int-uuid");
        data.putInt("bodycam_target_uuid", 42);
        when(((BodycamPersistentDataHolder) player).bodycam$getPersistentData()).thenReturn(data);

        try (MockedStatic<BodycamResetCameraPacket> resetMock = Mockito.mockStatic(BodycamResetCameraPacket.class);
             MockedStatic<PlatformCommonHelper> helperMock = Mockito.mockStatic(PlatformCommonHelper.class)) {

            ServerEvents.onPlayerLogin(player);

            verify(player, never()).teleportTo(
                    any(ServerLevel.class),
                    Mockito.doubleThat(v -> Double.isNaN(v)),
                    anyDouble(),
                    anyDouble(),
                    any(),
                    anyFloat(),
                    anyFloat()
            );
            verify(player).teleportTo(overworld, 0.5D, 64.0D, 0.5D, Set.of(), 0.0F, 0.0F);
            resetMock.verify(() -> BodycamResetCameraPacket.cleanupPlayerNbtAndMaps(player));
            helperMock.verify(() -> PlatformCommonHelper.clearEntityChunks(server, playerId));
        }
    }
}
