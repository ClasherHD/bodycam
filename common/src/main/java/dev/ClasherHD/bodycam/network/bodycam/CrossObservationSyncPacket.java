package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public class CrossObservationSyncPacket implements CustomPacketPayload {
    public static final Type<CrossObservationSyncPacket> TYPE = new Type<>(bodycam.id("cross_observation_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrossObservationSyncPacket> STREAM_CODEC =
            StreamCodec.of(CrossObservationSyncPacket::encode, CrossObservationSyncPacket::decode);

    public final UUID observerId;
    public final boolean observing;

    public CrossObservationSyncPacket(UUID observerId, boolean observing) {
        this.observerId = observerId;
        this.observing = observing;
    }

    public static void encode(RegistryFriendlyByteBuf buf, CrossObservationSyncPacket msg) {
        buf.writeUUID(msg.observerId);
        buf.writeBoolean(msg.observing);
    }

    public static CrossObservationSyncPacket decode(RegistryFriendlyByteBuf buf) {
        return new CrossObservationSyncPacket(buf.readUUID(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrossObservationSyncPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleCrossObservationSync(msg)));
    }
}
