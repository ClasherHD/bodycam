package dev.ClasherHD.bodycam.network.config;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.ClasherHD.bodycam.config.ServerConfigSnapshot;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class OpenServerConfigS2CPacket implements CustomPacketPayload {
    public static final Type<OpenServerConfigS2CPacket> TYPE = new Type<>(bodycam.id("open_server_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenServerConfigS2CPacket> STREAM_CODEC =
            StreamCodec.of(OpenServerConfigS2CPacket::encode, OpenServerConfigS2CPacket::decode);

    public final ServerConfigSnapshot snapshot;

    public OpenServerConfigS2CPacket(ServerConfigSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(RegistryFriendlyByteBuf buf, OpenServerConfigS2CPacket msg) {
        msg.snapshot.encode(buf);
    }

    public static OpenServerConfigS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenServerConfigS2CPacket(ServerConfigSnapshot.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenServerConfigS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleOpenServerConfig(msg)));
    }
}
