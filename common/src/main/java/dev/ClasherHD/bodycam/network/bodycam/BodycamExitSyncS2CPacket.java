package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BodycamExitSyncS2CPacket(
        double x,
        double y,
        double z,
        float yRot,
        float xRot,
        double vx,
        double vy,
        double vz,
        float fallDistance,
        int gameModeId,
        int removedDummyEntityId,
        int observerEntityId,
        boolean invisible
) implements CustomPacketPayload {

    public BodycamExitSyncS2CPacket(
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            double vx,
            double vy,
            double vz,
            float fallDistance,
            int gameModeId,
            int removedDummyEntityId
    ) {
        this(x, y, z, yRot, xRot, vx, vy, vz, fallDistance, gameModeId, removedDummyEntityId, -1, false);
    }

    public static final Type<BodycamExitSyncS2CPacket> TYPE = new Type<>(bodycam.id("bodycam_exit_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodycamExitSyncS2CPacket> STREAM_CODEC =
            StreamCodec.of(BodycamExitSyncS2CPacket::encode, BodycamExitSyncS2CPacket::decode);

    public static void encode(RegistryFriendlyByteBuf buf, BodycamExitSyncS2CPacket msg) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.xRot);
        buf.writeDouble(msg.vx);
        buf.writeDouble(msg.vy);
        buf.writeDouble(msg.vz);
        buf.writeFloat(msg.fallDistance);
        buf.writeVarInt(msg.gameModeId);
        buf.writeVarInt(msg.removedDummyEntityId);
        buf.writeVarInt(msg.observerEntityId);
        buf.writeBoolean(msg.invisible);
    }

    public static BodycamExitSyncS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new BodycamExitSyncS2CPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BodycamExitSyncS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleExitSync(msg)));
    }
}
