package dev.ClasherHD.bodycam.network.locator;

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
import java.util.Map;
import java.util.UUID;

public class PlayerLocatorSyncS2CPacket implements CustomPacketPayload {
    public static final Type<PlayerLocatorSyncS2CPacket> TYPE = new Type<>(bodycam.id("player_locator_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLocatorSyncS2CPacket> STREAM_CODEC =
            StreamCodec.of(PlayerLocatorSyncS2CPacket::encode, PlayerLocatorSyncS2CPacket::decode);

    public final Map<UUID, Integer> jammers;
    public final Map<UUID, String> dimensions;
    public final Map<UUID, BlockPos> positions;
    public final UUID currentTarget;
    public final int reachLevel;

    public PlayerLocatorSyncS2CPacket(Map<UUID, Integer> jammers,
                                      Map<UUID, String> dimensions,
                                      Map<UUID, BlockPos> positions,
                                      UUID currentTarget,
                                      int reachLevel) {
        this.jammers = jammers;
        this.dimensions = dimensions;
        this.positions = positions;
        this.currentTarget = currentTarget;
        this.reachLevel = reachLevel;
    }

    public static void encode(RegistryFriendlyByteBuf buf, PlayerLocatorSyncS2CPacket msg) {
        buf.writeInt(msg.jammers.size());
        for (Map.Entry<UUID, Integer> entry : msg.jammers.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeInt(msg.dimensions.size());
        for (Map.Entry<UUID, String> entry : msg.dimensions.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue(), 256);
        }
        buf.writeInt(msg.positions.size());
        for (Map.Entry<UUID, BlockPos> entry : msg.positions.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeBlockPos(entry.getValue());
        }
        buf.writeBoolean(msg.currentTarget != null);
        if (msg.currentTarget != null) {
            buf.writeUUID(msg.currentTarget);
        }
        buf.writeInt(msg.reachLevel);
    }

    public static PlayerLocatorSyncS2CPacket decode(RegistryFriendlyByteBuf buf) {
        Map<UUID, Integer> jammers = new HashMap<>();
        int jSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < jSize; i++) {
            jammers.put(buf.readUUID(), buf.readInt());
        }
        Map<UUID, String> dimensions = new HashMap<>();
        int dSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < dSize; i++) {
            dimensions.put(buf.readUUID(), buf.readUtf(256));
        }
        Map<UUID, BlockPos> positions = new HashMap<>();
        int pSize = Math.max(0, Math.min(buf.readInt(), 1024));
        for (int i = 0; i < pSize; i++) {
            positions.put(buf.readUUID(), buf.readBlockPos());
        }
        UUID currentTarget = buf.readBoolean() ? buf.readUUID() : null;
        int reachLevel = buf.readInt();
        return new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, currentTarget, reachLevel);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLocatorSyncS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handlePlayerLocatorSync(msg)));
    }
}
