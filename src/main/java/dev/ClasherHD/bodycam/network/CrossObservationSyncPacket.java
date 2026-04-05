package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

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


}
