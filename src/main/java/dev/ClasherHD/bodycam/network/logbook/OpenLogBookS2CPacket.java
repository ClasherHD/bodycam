package dev.ClasherHD.bodycam.network.logbook;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class OpenLogBookS2CPacket {
    public final CompoundTag data;

    public OpenLogBookS2CPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public static void encode(OpenLogBookS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static OpenLogBookS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenLogBookS2CPacket(buf.readNbt());
    }

    public static void handle(OpenLogBookS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleOpenLogBook(msg)));
        ctx.get().setPacketHandled(true);
    }
}
