package dev.ClasherHD.bodycam.platform;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlatformCommonHelper {
    private static final Map<ResourceKey<Level>, Map<Long, Set<String>>> FORCED_CHUNK_OWNERS = new ConcurrentHashMap<>();

    private PlatformCommonHelper() {
    }

    public static void forceEntityChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add) {
        if (owner == null) {
            return;
        }
        forceChunkWithOwner(level, "entity:" + owner, chunkX, chunkZ, add);
    }

    public static void forceBlockChunk(ServerLevel level, BlockPos ownerPos, int chunkX, int chunkZ, boolean add) {
        if (ownerPos == null) {
            return;
        }
        forceChunkWithOwner(level, "block:" + ownerPos.asLong(), chunkX, chunkZ, add);
    }

    private static void forceChunkWithOwner(ServerLevel level, String ownerKey, int chunkX, int chunkZ, boolean add) {
        if (level == null) {
            return;
        }
        long chunkLong = ChunkPos.asLong(chunkX, chunkZ);
        Map<Long, Set<String>> dimMap = FORCED_CHUNK_OWNERS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (add) {
            Set<String> owners = dimMap.computeIfAbsent(chunkLong, k -> ConcurrentHashMap.newKeySet());
            boolean wasEmpty = owners.isEmpty();
            owners.add(ownerKey);
            if (wasEmpty) {
                level.setChunkForced(chunkX, chunkZ, true);
            }
        } else {
            Set<String> owners = dimMap.get(chunkLong);
            if (owners != null) {
                owners.remove(ownerKey);
                if (owners.isEmpty()) {
                    dimMap.remove(chunkLong);
                    level.setChunkForced(chunkX, chunkZ, false);
                }
            } else {
                level.setChunkForced(chunkX, chunkZ, false);
            }
        }
    }

    public static void clearEntityChunks(MinecraftServer server, UUID owner) {
        if (owner == null) {
            return;
        }
        String ownerKey = "entity:" + owner;
        for (Map.Entry<ResourceKey<Level>, Map<Long, Set<String>>> dimEntry : FORCED_CHUNK_OWNERS.entrySet()) {
            ServerLevel level = server != null ? server.getLevel(dimEntry.getKey()) : null;
            Iterator<Map.Entry<Long, Set<String>>> chunkIt = dimEntry.getValue().entrySet().iterator();
            while (chunkIt.hasNext()) {
                Map.Entry<Long, Set<String>> chunkEntry = chunkIt.next();
                Set<String> owners = chunkEntry.getValue();
                if (owners.remove(ownerKey) && owners.isEmpty()) {
                    long chunkLong = chunkEntry.getKey();
                    chunkIt.remove();
                    if (level != null) {
                        level.setChunkForced(ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong), false);
                    }
                }
            }
        }
    }

    public static void clearAllForcedChunks(MinecraftServer server) {
        if (server != null) {
            for (Map.Entry<ResourceKey<Level>, Map<Long, Set<String>>> dimEntry : FORCED_CHUNK_OWNERS.entrySet()) {
                ServerLevel level = server.getLevel(dimEntry.getKey());
                if (level != null) {
                    for (Long chunkLong : dimEntry.getValue().keySet()) {
                        level.setChunkForced(ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong), false);
                    }
                }
            }
        }
        FORCED_CHUNK_OWNERS.clear();
    }

    public static void openExtendedMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        if (player == null || provider == null || pos == null) {
            return;
        }
        MenuRegistry.openExtendedMenu(player, provider, buf -> buf.writeBlockPos(pos));
    }

    public static void stopRidingAndSetCamera(ServerPlayer observer, Entity target) {
        if (observer == null || target == null) {
            return;
        }
        if (observer.isPassenger()) {
            observer.stopRiding();
        }
        observer.setCamera(target);
    }
}
