package dev.ClasherHD.bodycam.network.bodycam;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class SyncBodycamStatesS2CPacket {
    public final Map<UUID, Integer> jammers;
    public final Map<UUID, UUID> targets;
    public final Map<UUID, String> dimensions;
    public final Map<UUID, BlockPos> positions;
    public final Map<UUID, Boolean> anonymizers;
    public final Set<UUID> hidden;
    public final Map<UUID, String> watchPoints;
    public final int reachLevel;
    public final boolean isOnHologram;

    public SyncBodycamStatesS2CPacket(Map<UUID, Integer> jammers, Map<UUID, UUID> targets, Map<UUID, String> dimensions,
            Map<UUID, BlockPos> positions, Map<UUID, Boolean> anonymizers, Set<UUID> hidden, Map<UUID, String> watchPoints,
            int reachLevel, boolean isOnHologram) {
        this.jammers = jammers;
        this.targets = targets;
        this.dimensions = dimensions;
        this.positions = positions;
        this.anonymizers = anonymizers;
        this.hidden = hidden;
        this.watchPoints = watchPoints;
        this.reachLevel = reachLevel;
        this.isOnHologram = isOnHologram;
    }

    public static void encode(SyncBodycamStatesS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.jammers.size());
        for (Map.Entry<UUID, Integer> entry : msg.jammers.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeInt(entry.getValue());
        }

        buf.writeInt(msg.targets.size());
        for (Map.Entry<UUID, UUID> entry : msg.targets.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUUID(entry.getValue());
        }

        buf.writeInt(msg.dimensions.size());
        for (Map.Entry<UUID, String> entry : msg.dimensions.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }

        buf.writeInt(msg.positions.size());
        for (Map.Entry<UUID, BlockPos> entry : msg.positions.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeBlockPos(entry.getValue());
        }

        buf.writeInt(msg.anonymizers.size());
        for (Map.Entry<UUID, Boolean> entry : msg.anonymizers.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }

        buf.writeInt(msg.hidden.size());
        for (UUID id : msg.hidden) {
            buf.writeUUID(id);
        }

        buf.writeInt(msg.watchPoints.size());
        for (Map.Entry<UUID, String> entry : msg.watchPoints.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }

        buf.writeInt(msg.reachLevel);
        buf.writeBoolean(msg.isOnHologram);
    }

    public static SyncBodycamStatesS2CPacket decode(FriendlyByteBuf buf) {
        Map<UUID, Integer> jammers = new HashMap<>();
        int jammerSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < jammerSize; i++)
            jammers.put(buf.readUUID(), buf.readInt());

        Map<UUID, UUID> targets = new HashMap<>();
        int targetsSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < targetsSize; i++)
            targets.put(buf.readUUID(), buf.readUUID());

        Map<UUID, String> dimensions = new HashMap<>();
        int dimensionsSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < dimensionsSize; i++)
            dimensions.put(buf.readUUID(), buf.readUtf(256));

        Map<UUID, BlockPos> positions = new HashMap<>();
        int positionsSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < positionsSize; i++)
            positions.put(buf.readUUID(), buf.readBlockPos());

        Map<UUID, Boolean> anonymizers = new HashMap<>();
        int anonymizerSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < anonymizerSize; i++)
            anonymizers.put(buf.readUUID(), buf.readBoolean());

        Set<UUID> hidden = new HashSet<>();
        int hiddenSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < hiddenSize; i++)
            hidden.add(buf.readUUID());

        Map<UUID, String> watchPoints = new LinkedHashMap<>();
        int wpSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < wpSize; i++)
            watchPoints.put(buf.readUUID(), buf.readUtf(256));

        return new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints,
                buf.readInt(), buf.readBoolean());
    }

    public static void handle(SyncBodycamStatesS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleSyncStates(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
