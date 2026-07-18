package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public record DimensionLocatorResponsePacket(Map<UUID, String> dimensions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DimensionLocatorResponsePacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bodycam", "dim_locator_resp"));
    
    public static final StreamCodec<FriendlyByteBuf, DimensionLocatorResponsePacket> STREAM_CODEC = StreamCodec.of(
        DimensionLocatorResponsePacket::encode,
        DimensionLocatorResponsePacket::decode
    );

    private static void encode(FriendlyByteBuf buf, DimensionLocatorResponsePacket msg) {
        buf.writeInt(msg.dimensions().size());
        for (Map.Entry<UUID, String> entry : msg.dimensions().entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    private static DimensionLocatorResponsePacket decode(FriendlyByteBuf buf) {
        Map<UUID, String> dimensions = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            dimensions.put(buf.readUUID(), buf.readUtf());
        }
        return new DimensionLocatorResponsePacket(dimensions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
