package dev.ClasherHD.bodycam.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class PlayerLocatorSyncS2CPacket {
    public final Map<UUID, Integer> jammers;
    public final Map<UUID, String> dimensions;
    public final Map<UUID, BlockPos> positions;
    public final UUID currentTarget;
    public final boolean hasReach;

    public PlayerLocatorSyncS2CPacket(Map<UUID, Integer> jammers, Map<UUID, String> dimensions, Map<UUID, BlockPos> positions, UUID currentTarget, boolean hasReach) {
        this.jammers = jammers;
        this.dimensions = dimensions;
        this.positions = positions;
        this.currentTarget = currentTarget;
        this.hasReach = hasReach;
    }

    public static void encode(PlayerLocatorSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.jammers.size());
        for (Map.Entry<UUID, Integer> entry : msg.jammers.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeInt(entry.getValue());
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

        buf.writeBoolean(msg.currentTarget != null);
        if (msg.currentTarget != null) {
            buf.writeUUID(msg.currentTarget);
        }
        buf.writeBoolean(msg.hasReach);
    }

    public static PlayerLocatorSyncS2CPacket decode(FriendlyByteBuf buf) {
        Map<UUID, Integer> jammers = new HashMap<>();
        int jammerSize = buf.readInt();
        for (int i = 0; i < jammerSize; i++) {
            jammers.put(buf.readUUID(), buf.readInt());
        }

        Map<UUID, String> dimensions = new HashMap<>();
        int dimensionsSize = buf.readInt();
        for (int i = 0; i < dimensionsSize; i++) {
            dimensions.put(buf.readUUID(), buf.readUtf());
        }

        Map<UUID, BlockPos> positions = new HashMap<>();
        int positionsSize = buf.readInt();
        for (int i = 0; i < positionsSize; i++) {
            positions.put(buf.readUUID(), buf.readBlockPos());
        }

        UUID currentTarget = buf.readBoolean() ? buf.readUUID() : null;
        boolean hasReach = buf.readBoolean();

        return new PlayerLocatorSyncS2CPacket(jammers, dimensions, positions, currentTarget, hasReach);
    }

    public static void handle(PlayerLocatorSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.ClientPacketHandler.handlePlayerLocatorSync(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
