package dev.ClasherHD.bodycam.network.logbook;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class OpenLogBookScreenS2CPacket implements CustomPacketPayload {
    public static final Type<OpenLogBookScreenS2CPacket> TYPE = new Type<>(bodycam.id("open_logbook_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLogBookScreenS2CPacket> STREAM_CODEC =
            StreamCodec.of(OpenLogBookScreenS2CPacket::encode, OpenLogBookScreenS2CPacket::decode);

    public final CompoundTag data;

    public OpenLogBookScreenS2CPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public static void encode(RegistryFriendlyByteBuf buf, OpenLogBookScreenS2CPacket msg) {
        buf.writeNbt(msg.data);
    }

    public static OpenLogBookScreenS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenLogBookScreenS2CPacket(buf.readNbt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenLogBookScreenS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleOpenLogBookScreen(msg)));
    }
}
