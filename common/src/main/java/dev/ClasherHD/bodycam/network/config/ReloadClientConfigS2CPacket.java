package dev.ClasherHD.bodycam.network.config;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.config.ModClientConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ReloadClientConfigS2CPacket implements CustomPacketPayload {
    public static final Type<ReloadClientConfigS2CPacket> TYPE = new Type<>(bodycam.id("reload_client_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadClientConfigS2CPacket> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> {}, buf -> new ReloadClientConfigS2CPacket());

    public ReloadClientConfigS2CPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReloadClientConfigS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> ModClientConfig::reloadFromDisk));
    }
}
