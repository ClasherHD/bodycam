package dev.ClasherHD.bodycam.network.config;

import dev.ClasherHD.bodycam.config.ModClientConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ReloadClientConfigS2CPacket {

    public ReloadClientConfigS2CPacket() {
    }

    public static void encode(ReloadClientConfigS2CPacket msg, FriendlyByteBuf buf) {
    }

    public static ReloadClientConfigS2CPacket decode(FriendlyByteBuf buf) {
        return new ReloadClientConfigS2CPacket();
    }

    public static void handle(ReloadClientConfigS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ModClientConfig::reloadFromDisk));
        ctx.get().setPacketHandled(true);
    }
}
