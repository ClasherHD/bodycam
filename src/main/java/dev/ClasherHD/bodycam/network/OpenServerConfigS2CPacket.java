package dev.ClasherHD.bodycam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;

public class OpenServerConfigS2CPacket {
    public final int maxMonitorDistance;
    public final boolean enableReachEnchantment;
    public final boolean enableJammer;
    public final boolean enableDimensionLocator;
    public final boolean enableHologramBlock;
    public final boolean enableAnonymizer;
    public final boolean opOnlyMode;

    public OpenServerConfigS2CPacket(int maxMonitorDistance, boolean enableReachEnchantment, boolean enableJammer,
            boolean enableDimensionLocator, boolean enableHologramBlock, boolean enableAnonymizer, boolean opOnlyMode) {
        this.maxMonitorDistance = maxMonitorDistance;
        this.enableReachEnchantment = enableReachEnchantment;
        this.enableJammer = enableJammer;
        this.enableDimensionLocator = enableDimensionLocator;
        this.enableHologramBlock = enableHologramBlock;
        this.enableAnonymizer = enableAnonymizer;
        this.opOnlyMode = opOnlyMode;
    }

    public static void encode(OpenServerConfigS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maxMonitorDistance);
        buf.writeBoolean(msg.enableReachEnchantment);
        buf.writeBoolean(msg.enableJammer);
        buf.writeBoolean(msg.enableDimensionLocator);
        buf.writeBoolean(msg.enableHologramBlock);
        buf.writeBoolean(msg.enableAnonymizer);
        buf.writeBoolean(msg.opOnlyMode);
    }

    public static OpenServerConfigS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenServerConfigS2CPacket(
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(OpenServerConfigS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.ClientPacketHandler.handleOpenServerConfig(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
