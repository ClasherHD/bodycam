package dev.ClasherHD.bodycam.network.logbook;

import dev.ClasherHD.bodycam.bodycam;
import dev.ClasherHD.bodycam.client.network.ClientPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class OpenLogBookPinScreenS2CPacket implements CustomPacketPayload {
    public static final Type<OpenLogBookPinScreenS2CPacket> TYPE = new Type<>(bodycam.id("open_logbook_pin_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLogBookPinScreenS2CPacket> STREAM_CODEC =
            StreamCodec.of(OpenLogBookPinScreenS2CPacket::encode, OpenLogBookPinScreenS2CPacket::decode);

    public static final int MODE_UNLOCK = 0;
    public static final int MODE_SET = 1;
    public static final int MODE_CHANGE = 2;

    public final int mode;
    public final boolean offHand;

    public OpenLogBookPinScreenS2CPacket(int mode, boolean offHand) {
        this.mode = mode;
        this.offHand = offHand;
    }

    public static void encode(RegistryFriendlyByteBuf buf, OpenLogBookPinScreenS2CPacket msg) {
        buf.writeVarInt(msg.mode);
        buf.writeBoolean(msg.offHand);
    }

    public static OpenLogBookPinScreenS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenLogBookPinScreenS2CPacket(buf.readVarInt(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenLogBookPinScreenS2CPacket msg, NetworkManager.PacketContext context) {
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientPacketHandler.handleOpenLogBookPinScreen(msg)));
    }
}
