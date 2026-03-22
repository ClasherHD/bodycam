package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

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
            if (msg.isObserving) {
                dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.add(msg.observerId);
            } else {
                dev.ClasherHD.bodycam.client.gui.PlayerSelectionScreen.observingMe.remove(msg.observerId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
