package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@SuppressWarnings("null")
public class DimensionLocatorResponsePacket {

    public final Map<UUID, String> dimensions;

    public DimensionLocatorResponsePacket(Map<UUID, String> dimensions) {
        this.dimensions = dimensions;
    }

    public static void encode(DimensionLocatorResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.dimensions.size());
        for (Map.Entry<UUID, String> entry : msg.dimensions.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static DimensionLocatorResponsePacket decode(FriendlyByteBuf buf) {
        Map<UUID, String> dims = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            dims.put(buf.readUUID(), buf.readUtf());
        }
        return new DimensionLocatorResponsePacket(dims);
    }

    public static void handle(DimensionLocatorResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.ClientPacketHandler.handleDimensionResponse(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
