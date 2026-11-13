package dev.ClasherHD.bodycam.network.bodycam;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class BodycamForceClosePacket implements CustomPacketPayload {
    public static final Type<BodycamForceClosePacket> TYPE = new Type<>(bodycam.id("bodycam_force_close"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodycamForceClosePacket> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {}, buf -> new BodycamForceClosePacket());

    public BodycamForceClosePacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BodycamForceClosePacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> ClientPacketHandler::handleForceClose));
    }
}
