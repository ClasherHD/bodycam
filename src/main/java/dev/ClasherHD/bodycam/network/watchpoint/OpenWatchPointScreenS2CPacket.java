package dev.ClasherHD.bodycam.network.watchpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class OpenWatchPointScreenS2CPacket {
    public final BlockPos pos;
    public final String currentName;

    public OpenWatchPointScreenS2CPacket(BlockPos pos, String currentName) {
        this.pos = pos;
        this.currentName = currentName == null ? "" : currentName;
    }

    public static void encode(OpenWatchPointScreenS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.currentName, 32);
    }

    public static OpenWatchPointScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenWatchPointScreenS2CPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(OpenWatchPointScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleOpenWatchPointScreen(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
