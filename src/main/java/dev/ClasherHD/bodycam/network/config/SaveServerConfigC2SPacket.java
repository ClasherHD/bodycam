package dev.ClasherHD.bodycam.network.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveServerConfigC2SPacket {
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

    public SaveServerConfigC2SPacket(int maxMonitorDistance, int maxWatchPointsPerPlayer, int maxWatchPointsTotal,
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

    public static void encode(SaveServerConfigC2SPacket msg, FriendlyByteBuf buf) {
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

    public static SaveServerConfigC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveServerConfigC2SPacket(
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

    public static void handle(SaveServerConfigC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                dev.ClasherHD.bodycam.config.ModServerConfig.MAX_MONITOR_DISTANCE.set(Math.max(1, Math.min(1000000, msg.maxMonitorDistance)));
                dev.ClasherHD.bodycam.config.ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.set(Math.max(1, Math.min(100, msg.maxWatchPointsPerPlayer)));
                dev.ClasherHD.bodycam.config.ModServerConfig.MAX_WATCH_POINTS_TOTAL.set(Math.max(1, Math.min(1000, msg.maxWatchPointsTotal)));
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_REACH_ENCHANTMENT.set(msg.enableReachEnchantment);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_JAMMER.set(msg.enableJammer);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_DIMENSION_LOCATOR.set(msg.enableDimensionLocator);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_HOLOGRAM_BLOCK.set(msg.enableHologramBlock);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ANONYMIZER.set(msg.enableAnonymizer);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_PLAYER_LOCATOR.set(msg.enablePlayerLocator);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_WATCH_POINT.set(msg.enableWatchPoint);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_PODEST.set(msg.enablePodest);
                dev.ClasherHD.bodycam.config.ModServerConfig.OP_ONLY_MODE.set(msg.opOnlyMode);
                dev.ClasherHD.bodycam.config.ModServerConfig.SPEC.save();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
