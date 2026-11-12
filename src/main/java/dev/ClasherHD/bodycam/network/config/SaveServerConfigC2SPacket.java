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
    public final boolean enableLogger;
    public final boolean enablePortablePortal;
    public final boolean enableAllSeeingEye;
    public final boolean enableCreepingDeath;
    public final boolean enableScholarCurse;
    public final boolean enableCreepingDeathNuke;

    public SaveServerConfigC2SPacket(int maxMonitorDistance, int maxWatchPointsPerPlayer, int maxWatchPointsTotal,
            boolean enableReachEnchantment, boolean enableJammer, boolean enableDimensionLocator,
            boolean enableHologramBlock, boolean enableAnonymizer, boolean enablePlayerLocator,
            boolean enableWatchPoint, boolean enablePodest, boolean opOnlyMode,
            boolean enableLogger, boolean enablePortablePortal, boolean enableAllSeeingEye,
            boolean enableCreepingDeath, boolean enableScholarCurse, boolean enableCreepingDeathNuke) {
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
        this.enableLogger = enableLogger;
        this.enablePortablePortal = enablePortablePortal;
        this.enableAllSeeingEye = enableAllSeeingEye;
        this.enableCreepingDeath = enableCreepingDeath;
        this.enableScholarCurse = enableScholarCurse;
        this.enableCreepingDeathNuke = enableCreepingDeathNuke;
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
        buf.writeBoolean(msg.enableLogger);
        buf.writeBoolean(msg.enablePortablePortal);
        buf.writeBoolean(msg.enableAllSeeingEye);
        buf.writeBoolean(msg.enableCreepingDeath);
        buf.writeBoolean(msg.enableScholarCurse);
        buf.writeBoolean(msg.enableCreepingDeathNuke);
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
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_LOGGER.set(msg.enableLogger);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.set(msg.enablePortablePortal);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_ALL_SEEING_EYE.set(msg.enableAllSeeingEye);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_CREEPING_DEATH.set(msg.enableCreepingDeath);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_SCHOLAR_CURSE.set(msg.enableScholarCurse);
                dev.ClasherHD.bodycam.config.ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.set(msg.enableCreepingDeathNuke);
                dev.ClasherHD.bodycam.config.ModServerConfig.SPEC.save();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
