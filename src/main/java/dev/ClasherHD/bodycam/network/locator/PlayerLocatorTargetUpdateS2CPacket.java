package dev.ClasherHD.bodycam.network.locator;


import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class PlayerLocatorTargetUpdateS2CPacket {
    public final UUID targetUUID;
    public final BlockPos pos;
    public final String dimension;

    public PlayerLocatorTargetUpdateS2CPacket(UUID targetUUID, BlockPos pos, String dimension) {
        this.targetUUID = targetUUID;
        this.pos = pos;
        this.dimension = dimension;
    }

    public static void encode(PlayerLocatorTargetUpdateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetUUID);
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.dimension);
    }

    public static PlayerLocatorTargetUpdateS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayerLocatorTargetUpdateS2CPacket(buf.readUUID(), buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(PlayerLocatorTargetUpdateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handlePlayerLocatorTargetUpdate(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}