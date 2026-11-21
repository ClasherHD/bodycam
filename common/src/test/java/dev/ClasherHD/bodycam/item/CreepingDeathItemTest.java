package dev.ClasherHD.bodycam.item;

import com.mojang.authlib.GameProfile;
import dev.ClasherHD.bodycam.network.creepingdeath.OpenCreepingDeathScreenS2CPacket;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CreepingDeathItemTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
    }

    @AfterEach
    void cleanUp() {
        CreepingDeathItem.invalidateKnownPlayersCache();
    }

    @Test
    void collectAllKnownPlayers_loadsOfflinePlayersAsynchronouslyFromUsercacheAndPlayerdata(@TempDir Path tempDir) throws Exception {
        UUID offlineFromUsercache = UUID.randomUUID();
        UUID offlineFromDatOnly = UUID.randomUUID();

        String userCacheJson = "[{\"name\":\"CachedPlayer\",\"uuid\":\"" + offlineFromUsercache + "\"}]";
        Files.writeString(tempDir.resolve("usercache.json"), userCacheJson, StandardCharsets.UTF_8);

        Path playerDataDir = tempDir.resolve("playerdata");
        Files.createDirectories(playerDataDir);
        Files.writeString(playerDataDir.resolve(offlineFromDatOnly + ".dat"), "", StandardCharsets.UTF_8);

        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        GameProfileCache profileCache = mock(GameProfileCache.class);
        when(playerList.getPlayers()).thenReturn(List.of());
        when(server.getPlayerList()).thenReturn(playerList);
        when(server.getServerDirectory()).thenReturn(tempDir);
        when(server.getWorldPath(LevelResource.PLAYER_DATA_DIR)).thenReturn(playerDataDir);
        when(server.getProfileCache()).thenReturn(profileCache);
        when(profileCache.get(offlineFromDatOnly)).thenReturn(Optional.of(new GameProfile(offlineFromDatOnly, "DatPlayer")));
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(server).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        // Initial call returns immediately without blocking on disk I/O
        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> initial = CreepingDeathItem.collectAllKnownPlayers(server);
        assertThat(initial).isEmpty();

        // Wait briefly for the async ioPool scan to populate the cache
        List<OpenCreepingDeathScreenS2CPacket.PlayerEntryData> populated = List.of();
        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            populated = CreepingDeathItem.collectAllKnownPlayers(server);
            if (populated.size() == 2) {
                break;
            }
            Thread.sleep(20L);
        }

        assertThat(populated).extracting(OpenCreepingDeathScreenS2CPacket.PlayerEntryData::name)
                .containsExactly("CachedPlayer", "DatPlayer");
        assertThat(populated).extracting(OpenCreepingDeathScreenS2CPacket.PlayerEntryData::uuid)
                .containsExactly(offlineFromUsercache, offlineFromDatOnly);
    }
}
