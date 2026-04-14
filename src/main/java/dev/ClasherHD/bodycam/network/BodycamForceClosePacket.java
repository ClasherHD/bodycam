package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class BodycamForceClosePacket {

    public BodycamForceClosePacket() {
    }

    public static void encode(BodycamForceClosePacket msg, FriendlyByteBuf buf) {
    }

    public static BodycamForceClosePacket decode(FriendlyByteBuf buf) {
        return new BodycamForceClosePacket();
    }

    public static void handle(BodycamForceClosePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.ClientPacketHandler.handleForceClose(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
