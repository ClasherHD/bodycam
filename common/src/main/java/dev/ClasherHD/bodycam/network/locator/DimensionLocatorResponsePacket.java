package dev.ClasherHD.bodycam.network.locator;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DimensionLocatorResponsePacket implements CustomPacketPayload {
    public static final Type<DimensionLocatorResponsePacket> TYPE = new Type<>(bodycam.id("dimension_locator_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionLocatorResponsePacket> STREAM_CODEC =
            StreamCodec.of(DimensionLocatorResponsePacket::encode, DimensionLocatorResponsePacket::decode);

    public final Map<UUID, String> playerDimensions;

    public DimensionLocatorResponsePacket(Map<UUID, String> playerDimensions) {
        this.playerDimensions = playerDimensions == null ? Map.of() : playerDimensions;
    }

    public static void encode(RegistryFriendlyByteBuf buf, DimensionLocatorResponsePacket msg) {
        buf.writeInt(msg.playerDimensions.size());
        for (Map.Entry<UUID, String> e : msg.playerDimensions.entrySet()) {
            buf.writeUUID(e.getKey());
            buf.writeUtf(e.getValue(), 256);
        }
    }

    public static DimensionLocatorResponsePacket decode(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(buf.readInt(), 1024));
        Map<UUID, String> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUUID(), buf.readUtf(256));
        }
        return new DimensionLocatorResponsePacket(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DimensionLocatorResponsePacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleDimensionLocatorResponse(msg)));
    }
}
