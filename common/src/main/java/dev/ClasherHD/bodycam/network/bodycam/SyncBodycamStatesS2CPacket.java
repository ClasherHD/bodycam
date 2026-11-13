package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SyncBodycamStatesS2CPacket implements CustomPacketPayload {
    public static final Type<SyncBodycamStatesS2CPacket> TYPE = new Type<>(bodycam.id("sync_bodycam_states"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBodycamStatesS2CPacket> STREAM_CODEC =
            StreamCodec.of(SyncBodycamStatesS2CPacket::encode, SyncBodycamStatesS2CPacket::decode);

    public final Map<UUID, Integer> jammers;
    public final Map<UUID, UUID> targets;
    public final Map<UUID, String> dimensions;
    public final Map<UUID, BlockPos> positions;
    public final Map<UUID, Boolean> anonymizers;
    public final Set<UUID> hidden;
    public final Map<UUID, String> watchPoints;
    public final int reachLevel;
    public final boolean isOnHologram;
    public final boolean isAllSeeingEye;

    public SyncBodycamStatesS2CPacket(Map<UUID, Integer> jammers,
                                      Map<UUID, UUID> targets,
                                      Map<UUID, String> dimensions,
                                      Map<UUID, BlockPos> positions,
                                      Map<UUID, Boolean> anonymizers,
                                      Set<UUID> hidden,
                                      Map<UUID, String> watchPoints,
                                      int reachLevel,
                                      boolean isOnHologram) {
        this(jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, reachLevel, isOnHologram, false);
    }

    public SyncBodycamStatesS2CPacket(Map<UUID, Integer> jammers,
                                      Map<UUID, UUID> targets,
                                      Map<UUID, String> dimensions,
                                      Map<UUID, BlockPos> positions,
                                      Map<UUID, Boolean> anonymizers,
                                      Set<UUID> hidden,
                                      Map<UUID, String> watchPoints,
                                      int reachLevel,
                                      boolean isOnHologram,
                                      boolean isAllSeeingEye) {
        this.jammers = jammers;
        this.targets = targets;
        this.dimensions = dimensions;
        this.positions = positions;
        this.anonymizers = anonymizers;
        this.hidden = hidden;
        this.watchPoints = watchPoints;
        this.reachLevel = reachLevel;
        this.isOnHologram = isOnHologram;
        this.isAllSeeingEye = isAllSeeingEye;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SyncBodycamStatesS2CPacket msg) {
        buf.writeInt(msg.jammers.size());
        for (Map.Entry<UUID, Integer> e : msg.jammers.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeInt(e.getValue());
        }
        buf.writeInt(msg.targets.size());
        for (Map.Entry<UUID, UUID> e : msg.targets.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUUID(e.getValue());
        }
        buf.writeInt(msg.dimensions.size());
        for (Map.Entry<UUID, String> e : msg.dimensions.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUtf(e.getValue());
        }
        buf.writeInt(msg.positions.size());
        for (Map.Entry<UUID, BlockPos> e : msg.positions.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeBlockPos(e.getValue());
        }
        buf.writeInt(msg.anonymizers.size());
        for (Map.Entry<UUID, Boolean> e : msg.anonymizers.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeBoolean(e.getValue());
        }
        buf.writeInt(msg.hidden.size());
        for (UUID id : msg.hidden) {
            buf.writeUUID(id);
        }
        buf.writeInt(msg.watchPoints.size());
        for (Map.Entry<UUID, String> e : msg.watchPoints.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUtf(e.getValue());
        }
        buf.writeInt(msg.reachLevel);
        buf.writeBoolean(msg.isOnHologram);
        buf.writeBoolean(msg.isAllSeeingEye);
    }

    public static SyncBodycamStatesS2CPacket decode(RegistryFriendlyByteBuf buf) {
        Map<UUID, Integer> jammers = new HashMap<>();
        int jammerSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < jammerSize; i++) {
            jammers.put(buf.readUUID(), buf.readInt());
        }
        Map<UUID, UUID> targets = new HashMap<>();
        int targetSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < targetSize; i++) {
            targets.put(buf.readUUID(), buf.readUUID());
        }
        Map<UUID, String> dimensions = new HashMap<>();
        int dimensionsSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < dimensionsSize; i++) {
            dimensions.put(buf.readUUID(), buf.readUtf(256));
        }
        Map<UUID, BlockPos> positions = new HashMap<>();
        int positionsSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < positionsSize; i++) {
            positions.put(buf.readUUID(), buf.readBlockPos());
        }
        Map<UUID, Boolean> anonymizers = new HashMap<>();
        int anonymizerSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < anonymizerSize; i++) {
            anonymizers.put(buf.readUUID(), buf.readBoolean());
        }
        Set<UUID> hidden = new HashSet<>();
        int hiddenSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < hiddenSize; i++) {
            hidden.add(buf.readUUID());
        }
        Map<UUID, String> watchPoints = new LinkedHashMap<>();
        int wpSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < wpSize; i++) {
            watchPoints.put(buf.readUUID(), buf.readUtf(256));
        }
        int reachLevel = buf.readInt();
        boolean isOnHologram = buf.readBoolean();
        boolean isAllSeeingEye = buf.readBoolean();
        return new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, hidden, watchPoints, reachLevel, isOnHologram, isAllSeeingEye);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBodycamStatesS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleSyncStates(msg)));
    }
}
