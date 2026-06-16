package dev.ClasherHD.bodycam.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class PlayerLocatorStructureUpdateS2CPacket {
    public final BlockPos structurePos;
    public final String dimension;

    public PlayerLocatorStructureUpdateS2CPacket(BlockPos structurePos, String dimension) {
        this.structurePos = structurePos;
        this.dimension = dimension;
    }

    public static void encode(PlayerLocatorStructureUpdateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.structurePos);
        buf.writeUtf(msg.dimension);
    }

    public static PlayerLocatorStructureUpdateS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayerLocatorStructureUpdateS2CPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(PlayerLocatorStructureUpdateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.ClientPacketHandler.handlePlayerLocatorStructureUpdate(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
