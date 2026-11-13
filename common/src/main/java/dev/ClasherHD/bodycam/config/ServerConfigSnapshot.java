package dev.ClasherHD.bodycam.config;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record ServerConfigSnapshot(
        int maxMonitorDistance,
        int maxWatchPointsPerPlayer,
        int maxWatchPointsTotal,
        boolean enableReachEnchantment,
        boolean enableJammer,
        boolean enableDimensionLocator,
        boolean enableHologramBlock,
        boolean enableAnonymizer,
        boolean enablePlayerLocator,
        boolean enableWatchPoint,
        boolean enablePodest,
        boolean opOnlyMode,
        boolean enableLogger,
        boolean enablePortablePortal,
        boolean enableAllSeeingEye,
        boolean enableCreepingDeath,
        boolean enableScholarCurse,
        boolean enableCreepingDeathNuke
) {
    public static ServerConfigSnapshot fromConfig() {
        return new ServerConfigSnapshot(
                ModServerConfig.MAX_MONITOR_DISTANCE.get(),
                ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.get(),
                ModServerConfig.MAX_WATCH_POINTS_TOTAL.get(),
                ModServerConfig.ENABLE_REACH_ENCHANTMENT.get(),
                ModServerConfig.ENABLE_JAMMER.get(),
                ModServerConfig.ENABLE_DIMENSION_LOCATOR.get(),
                ModServerConfig.ENABLE_HOLOGRAM_BLOCK.get(),
                ModServerConfig.ENABLE_ANONYMIZER.get(),
                ModServerConfig.ENABLE_PLAYER_LOCATOR.get(),
                ModServerConfig.ENABLE_WATCH_POINT.get(),
                ModServerConfig.ENABLE_PODEST.get(),
                ModServerConfig.OP_ONLY_MODE.get(),
                ModServerConfig.ENABLE_LOGGER.get(),
                ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.get(),
                ModServerConfig.ENABLE_ALL_SEEING_EYE.get(),
                ModServerConfig.ENABLE_CREEPING_DEATH.get(),
                ModServerConfig.ENABLE_SCHOLAR_CURSE.get(),
                ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.get()
        );
    }

    public static ServerConfigSnapshot defaults() {
        return new ServerConfigSnapshot(
                ModServerConfig.MAX_MONITOR_DISTANCE.getDefault(),
                ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.getDefault(),
                ModServerConfig.MAX_WATCH_POINTS_TOTAL.getDefault(),
                ModServerConfig.ENABLE_REACH_ENCHANTMENT.getDefault(),
                ModServerConfig.ENABLE_JAMMER.getDefault(),
                ModServerConfig.ENABLE_DIMENSION_LOCATOR.getDefault(),
                ModServerConfig.ENABLE_HOLOGRAM_BLOCK.getDefault(),
                ModServerConfig.ENABLE_ANONYMIZER.getDefault(),
                ModServerConfig.ENABLE_PLAYER_LOCATOR.getDefault(),
                ModServerConfig.ENABLE_WATCH_POINT.getDefault(),
                ModServerConfig.ENABLE_PODEST.getDefault(),
                ModServerConfig.OP_ONLY_MODE.getDefault(),
                ModServerConfig.ENABLE_LOGGER.getDefault(),
                ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.getDefault(),
                ModServerConfig.ENABLE_ALL_SEEING_EYE.getDefault(),
                ModServerConfig.ENABLE_CREEPING_DEATH.getDefault(),
                ModServerConfig.ENABLE_SCHOLAR_CURSE.getDefault(),
                ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.getDefault()
        );
    }

    public void applyToConfigAndSave() {
        ModServerConfig.MAX_MONITOR_DISTANCE.set(Math.max(1, Math.min(1000000, maxMonitorDistance)));
        ModServerConfig.MAX_WATCH_POINTS_PER_PLAYER.set(Math.max(1, Math.min(100, maxWatchPointsPerPlayer)));
        ModServerConfig.MAX_WATCH_POINTS_TOTAL.set(Math.max(1, Math.min(1000, maxWatchPointsTotal)));
        ModServerConfig.ENABLE_REACH_ENCHANTMENT.set(enableReachEnchantment);
        ModServerConfig.ENABLE_JAMMER.set(enableJammer);
        ModServerConfig.ENABLE_DIMENSION_LOCATOR.set(enableDimensionLocator);
        ModServerConfig.ENABLE_HOLOGRAM_BLOCK.set(enableHologramBlock);
        ModServerConfig.ENABLE_ANONYMIZER.set(enableAnonymizer);
        ModServerConfig.ENABLE_PLAYER_LOCATOR.set(enablePlayerLocator);
        ModServerConfig.ENABLE_WATCH_POINT.set(enableWatchPoint);
        ModServerConfig.ENABLE_PODEST.set(enablePodest);
        ModServerConfig.OP_ONLY_MODE.set(opOnlyMode);
        ModServerConfig.ENABLE_LOGGER.set(enableLogger);
        ModServerConfig.ENABLE_PORTABLE_NETHER_PORTAL.set(enablePortablePortal);
        ModServerConfig.ENABLE_ALL_SEEING_EYE.set(enableAllSeeingEye);
        ModServerConfig.ENABLE_CREEPING_DEATH.set(enableCreepingDeath);
        ModServerConfig.ENABLE_SCHOLAR_CURSE.set(enableScholarCurse);
        ModServerConfig.ENABLE_CREEPING_DEATH_NUKE.set(enableCreepingDeathNuke);
        ModServerConfig.save();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(maxMonitorDistance);
        buf.writeInt(maxWatchPointsPerPlayer);
        buf.writeInt(maxWatchPointsTotal);
        buf.writeBoolean(enableReachEnchantment);
        buf.writeBoolean(enableJammer);
        buf.writeBoolean(enableDimensionLocator);
        buf.writeBoolean(enableHologramBlock);
        buf.writeBoolean(enableAnonymizer);
        buf.writeBoolean(enablePlayerLocator);
        buf.writeBoolean(enableWatchPoint);
        buf.writeBoolean(enablePodest);
        buf.writeBoolean(opOnlyMode);
        buf.writeBoolean(enableLogger);
        buf.writeBoolean(enablePortablePortal);
        buf.writeBoolean(enableAllSeeingEye);
        buf.writeBoolean(enableCreepingDeath);
        buf.writeBoolean(enableScholarCurse);
        buf.writeBoolean(enableCreepingDeathNuke);
    }

    public static ServerConfigSnapshot decode(RegistryFriendlyByteBuf buf) {
        return new ServerConfigSnapshot(
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
}
