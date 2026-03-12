package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
            try {
                dev.ClasherHD.bodycam.client.gui.BodycamViewScreen.isMonitoring = false;
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    mc.setCameraEntity(mc.player);
                }
                mc.setScreen(null);
            } catch (Exception e) {
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
