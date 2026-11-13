package dev.ClasherHD.bodycam.network.config;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class OpenClientConfigS2CPacket implements CustomPacketPayload {
    public static final Type<OpenClientConfigS2CPacket> TYPE = new Type<>(bodycam.id("open_client_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClientConfigS2CPacket> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {}, buf -> new OpenClientConfigS2CPacket());

    public OpenClientConfigS2CPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenClientConfigS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> ClientPacketHandler::handleOpenClientConfig));
    }
}
