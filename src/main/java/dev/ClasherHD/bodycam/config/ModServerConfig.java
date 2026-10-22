package dev.ClasherHD.bodycam.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MAX_MONITOR_DISTANCE;
    public static final ForgeConfigSpec.IntValue PORTAL_SEARCH_CHUNK_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_WATCH_POINTS_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_WATCH_POINTS_TOTAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_REACH_ENCHANTMENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JAMMER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIMENSION_LOCATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOLOGRAM_BLOCK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ANONYMIZER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PLAYER_LOCATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WATCH_POINT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PODEST;
    public static final ForgeConfigSpec.BooleanValue OP_ONLY_MODE;

    static {
        BUILDER.push("Distance & Limits");
        MAX_MONITOR_DISTANCE = BUILDER.comment("Max observation distance without reach enchantment").defineInRange("maxMonitorDistance", 500, 1, 1000000);
        PORTAL_SEARCH_CHUNK_RADIUS = BUILDER.comment("Max chunk radius to scan for active portal blocks in Player Locator").defineInRange("portalSearchChunkRadius", 8, 1, 32);
        MAX_WATCH_POINTS_PER_PLAYER = BUILDER.comment("Max active Watch Point dummies per player").defineInRange("maxWatchPointsPerPlayer", 5, 1, 100);
        MAX_WATCH_POINTS_TOTAL = BUILDER.comment("Max active Watch Point dummies across the entire server").defineInRange("maxWatchPointsTotal", 50, 1, 1000);
        BUILDER.pop();

        BUILDER.push("Module Toggles");
        ENABLE_REACH_ENCHANTMENT = BUILDER.comment("Enable or Disable the Reach enchant capability").define("enableReachEnchantment", true);
        ENABLE_JAMMER = BUILDER.comment("Enable or Disable the Jammer item").define("enableJammer", true);
        ENABLE_DIMENSION_LOCATOR = BUILDER.comment("Enable or Disable the Dimension Locator GUI item").define("enableDimensionLocator", true);
        ENABLE_HOLOGRAM_BLOCK = BUILDER.comment("Enable or Disable the Hologram Cross-Dimension Spawner Block").define("enableHologramBlock", true);
        ENABLE_ANONYMIZER = BUILDER.comment("Enable or Disable the Anonymizer").define("enableAnonymizer", true);
        ENABLE_PLAYER_LOCATOR = BUILDER.comment("Enable or Disable the Player Locator Compass").define("enablePlayerLocator", true);
        ENABLE_WATCH_POINT = BUILDER.comment("Enable or Disable the Watch Point Block").define("enableWatchPoint", true);
        ENABLE_PODEST = BUILDER.comment("Enable or Disable the Podest Block").define("enablePodest", true);
        BUILDER.pop();

        BUILDER.push("Security");
        OP_ONLY_MODE = BUILDER.comment("Require Operator (Level 2) permissions to use ANY bodycam item/block mechanics").define("opOnlyMode", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
