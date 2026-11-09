package dev.ClasherHD.bodycam.network.logbook;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class OpenLogBookPinScreenS2CPacket {
    public static final int MODE_UNLOCK = 0;
    public static final int MODE_SET = 1;
    public static final int MODE_CHANGE = 2;

    public final int mode;
    public final boolean offHand;

    public OpenLogBookPinScreenS2CPacket(int mode, boolean offHand) {
        this.mode = mode;
        this.offHand = offHand;
    }

    public static void encode(OpenLogBookPinScreenS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.mode);
        buf.writeBoolean(msg.offHand);
    }

    public static OpenLogBookPinScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenLogBookPinScreenS2CPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(OpenLogBookPinScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleOpenLogBookPinScreen(msg)));
        ctx.get().setPacketHandled(true);
    }
}
