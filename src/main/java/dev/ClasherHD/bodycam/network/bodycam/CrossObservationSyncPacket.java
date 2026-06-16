package dev.ClasherHD.bodycam.network.bodycam;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@SuppressWarnings("null")
public class CrossObservationSyncPacket {
    public final UUID observerId;
    public final boolean isObserving;

    public CrossObservationSyncPacket(UUID observerId, boolean isObserving) {
        this.observerId = observerId;
        this.isObserving = isObserving;
    }

    public static void encode(CrossObservationSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.observerId);
        buf.writeBoolean(msg.isObserving);
    }

    public static CrossObservationSyncPacket decode(FriendlyByteBuf buf) {
        return new CrossObservationSyncPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(CrossObservationSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleCrossSync(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}