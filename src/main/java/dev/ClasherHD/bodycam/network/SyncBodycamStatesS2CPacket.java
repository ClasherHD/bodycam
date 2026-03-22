package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

public class SyncBodycamStatesS2CPacket {
    public final Map<UUID, Integer> jammers;
    public final Map<UUID, UUID> targets;
    public final Map<UUID, String> dimensions;
    public final Map<UUID, BlockPos> positions;
    public final Map<UUID, Boolean> anonymizers;
    public final boolean hasReach;
    public final boolean isOnHologram;

    public SyncBodycamStatesS2CPacket(Map<UUID, Integer> jammers, Map<UUID, UUID> targets, Map<UUID, String> dimensions, Map<UUID, BlockPos> positions, Map<UUID, Boolean> anonymizers, boolean hasReach, boolean isOnHologram) {
        this.jammers = jammers;
        this.targets = targets;
        this.dimensions = dimensions;
        this.positions = positions;
        this.anonymizers = anonymizers;
        this.hasReach = hasReach;
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

        buf.writeBoolean(msg.hasReach);
        buf.writeBoolean(msg.isOnHologram);
    }

    public static SyncBodycamStatesS2CPacket decode(FriendlyByteBuf buf) {
        Map<UUID, Integer> jammers = new HashMap<>();
        int jammerSize = buf.readInt();
        for (int i = 0; i < jammerSize; i++) jammers.put(buf.readUUID(), buf.readInt());

        Map<UUID, UUID> targets = new HashMap<>();
        int targetsSize = buf.readInt();
        for (int i = 0; i < targetsSize; i++) targets.put(buf.readUUID(), buf.readUUID());

        Map<UUID, String> dimensions = new HashMap<>();
        int dimensionsSize = buf.readInt();
        for (int i = 0; i < dimensionsSize; i++) dimensions.put(buf.readUUID(), buf.readUtf());

        Map<UUID, BlockPos> positions = new HashMap<>();
        int positionsSize = buf.readInt();
        for (int i = 0; i < positionsSize; i++) positions.put(buf.readUUID(), buf.readBlockPos());

        Map<UUID, Boolean> anonymizers = new HashMap<>();
        int anonymizerSize = buf.readInt();
        for (int i = 0; i < anonymizerSize; i++) anonymizers.put(buf.readUUID(), buf.readBoolean());

        return new SyncBodycamStatesS2CPacket(jammers, targets, dimensions, positions, anonymizers, buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SyncBodycamStatesS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            dev.ClasherHD.bodycam.client.ClientBodycamCache.update(msg.jammers, msg.targets, msg.dimensions, msg.positions, msg.anonymizers);
            Minecraft.getInstance().setScreen(new dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen(msg.hasReach, msg.isOnHologram));
        });
        ctx.get().setPacketHandled(true);
    }
}
