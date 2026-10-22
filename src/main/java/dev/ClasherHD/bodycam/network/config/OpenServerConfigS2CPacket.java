package dev.ClasherHD.bodycam.network.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenServerConfigS2CPacket {
    public final int maxMonitorDistance;
    public final int maxWatchPointsPerPlayer;
    public final int maxWatchPointsTotal;
    public final boolean enableReachEnchantment;
    public final boolean enableJammer;
    public final boolean enableDimensionLocator;
    public final boolean enableHologramBlock;
    public final boolean enableAnonymizer;
    public final boolean enablePlayerLocator;
    public final boolean enableWatchPoint;
    public final boolean enablePodest;
    public final boolean opOnlyMode;

    public OpenServerConfigS2CPacket(int maxMonitorDistance, int maxWatchPointsPerPlayer, int maxWatchPointsTotal,
            boolean enableReachEnchantment, boolean enableJammer, boolean enableDimensionLocator,
            boolean enableHologramBlock, boolean enableAnonymizer, boolean enablePlayerLocator,
            boolean enableWatchPoint, boolean enablePodest, boolean opOnlyMode) {
        this.maxMonitorDistance = maxMonitorDistance;
        this.maxWatchPointsPerPlayer = maxWatchPointsPerPlayer;
        this.maxWatchPointsTotal = maxWatchPointsTotal;
        this.enableReachEnchantment = enableReachEnchantment;
        this.enableJammer = enableJammer;
        this.enableDimensionLocator = enableDimensionLocator;
        this.enableHologramBlock = enableHologramBlock;
        this.enableAnonymizer = enableAnonymizer;
        this.enablePlayerLocator = enablePlayerLocator;
        this.enableWatchPoint = enableWatchPoint;
        this.enablePodest = enablePodest;
        this.opOnlyMode = opOnlyMode;
    }

    public static void encode(OpenServerConfigS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maxMonitorDistance);
        buf.writeInt(msg.maxWatchPointsPerPlayer);
        buf.writeInt(msg.maxWatchPointsTotal);
        buf.writeBoolean(msg.enableReachEnchantment);
        buf.writeBoolean(msg.enableJammer);
        buf.writeBoolean(msg.enableDimensionLocator);
        buf.writeBoolean(msg.enableHologramBlock);
        buf.writeBoolean(msg.enableAnonymizer);
        buf.writeBoolean(msg.enablePlayerLocator);
        buf.writeBoolean(msg.enableWatchPoint);
        buf.writeBoolean(msg.enablePodest);
        buf.writeBoolean(msg.opOnlyMode);
    }

    public static OpenServerConfigS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenServerConfigS2CPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ClasherHD.bodycam.client.network.ClientPacketHandler.handleOpenServerConfig(msg));
        });
        ctx.get().setPacketHandled(true);
    }
}
