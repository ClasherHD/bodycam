package dev.ClasherHD.bodycam.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class ModServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue OP_ONLY_MODE;
    public static final ForgeConfigSpec.IntValue MAX_MONITOR_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_REACH_ENCHANTMENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_JAMMER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIMENSION_LOCATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOLOGRAM_BLOCK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ANONYMIZER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PLAYER_LOCATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WATCH_POINT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PODEST;

    public static final ForgeConfigSpec.IntValue SPYGLASS_DURABILITY;
    public static final ForgeConfigSpec.IntValue DIMENSION_LOCATOR_DURABILITY;
    public static final ForgeConfigSpec.IntValue PLAYER_LOCATOR_DURABILITY;
    public static final ForgeConfigSpec.IntValue ANONYMIZER_DURABILITY;
    public static final ForgeConfigSpec.IntValue JAMMER_DURABILITY;
    public static final ForgeConfigSpec.IntValue ANCIENT_TABLE_FUEL_PER_CRYSTAL;
    public static final ForgeConfigSpec.IntValue ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL;
    public static final ForgeConfigSpec.IntValue ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM;

    public static final ForgeConfigSpec.IntValue MAX_WATCH_POINTS_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_WATCH_POINTS_TOTAL;

    public static final ForgeConfigSpec.DoubleValue PODEST_BLOCKS_PER_CHARGE;
    public static final ForgeConfigSpec.IntValue PODEST_JAMMER_NORMAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue PODEST_JAMMER_LIMITED_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue PODEST_ANONYMIZER_INTERVAL_TICKS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_DIMENSIONS;

    public static final ForgeConfigSpec.IntValue PORTAL_SEARCH_CHUNK_RADIUS;
    public static final ForgeConfigSpec.IntValue LOCATOR_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue PODEST_SPHERE_CHECK_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue JAMMER_NORMAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue JAMMER_LIMITED_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue ANONYMIZER_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS;

    private static ModConfig activeModConfig;

    static {
        BUILDER.push("general");
        OP_ONLY_MODE = BUILDER.comment("Require Operator (Level 2) permissions to use ANY bodycam item/block mechanics")
                .define("opOnlyMode", false);
        MAX_MONITOR_DISTANCE = BUILDER.comment("Max observation distance without reach enchantment")
                .defineInRange("maxMonitorDistance", 500, 1, 1000000);
        ENABLE_REACH_ENCHANTMENT = BUILDER.comment("Enable or Disable the Reach enchant capability")
                .define("enableReachEnchantment", true);
        ENABLE_JAMMER = BUILDER.comment("Enable or Disable the Jammer item")
                .define("enableJammer", true);
        ENABLE_DIMENSION_LOCATOR = BUILDER.comment("Enable or Disable the Dimension Locator GUI item")
                .define("enableDimensionLocator", true);
        ENABLE_HOLOGRAM_BLOCK = BUILDER.comment("Enable or Disable the Hologram Cross-Dimension Spawner Block")
                .define("enableHologramBlock", true);
        ENABLE_ANONYMIZER = BUILDER.comment("Enable or Disable the Anonymizer")
                .define("enableAnonymizer", true);
        ENABLE_PLAYER_LOCATOR = BUILDER.comment("Enable or Disable the Player Locator Compass")
                .define("enablePlayerLocator", true);
        ENABLE_WATCH_POINT = BUILDER.comment("Enable or Disable the Watch Point Block")
                .define("enableWatchPoint", true);
        ENABLE_PODEST = BUILDER.comment("Enable or Disable the Podest Block")
                .define("enablePodest", true);
        BUILDER.pop();

        BUILDER.push("durability");
        SPYGLASS_DURABILITY = BUILDER.comment("Max durability (uses) for the Observation Spyglass")
                .defineInRange("spyglassDurability", 16, 1, 100000);
        DIMENSION_LOCATOR_DURABILITY = BUILDER.comment("Max durability (uses) for the Dimension Locator")
                .defineInRange("dimensionLocatorDurability", 8, 1, 100000);
        PLAYER_LOCATOR_DURABILITY = BUILDER.comment("Max durability (seconds) for the Player Locator")
                .defineInRange("playerLocatorDurability", 560, 1, 100000);
        ANONYMIZER_DURABILITY = BUILDER.comment("Max durability (seconds) for the Anonymizer")
                .defineInRange("anonymizerDurability", 448, 1, 100000);
        JAMMER_DURABILITY = BUILDER.comment("Max durability points for the Jammer")
                .defineInRange("jammerDurability", 336, 1, 100000);
        ANCIENT_TABLE_FUEL_PER_CRYSTAL = BUILDER.comment("Durability points restored to per-time items by 1 Observation Crystal")
                .defineInRange("ancientTableFuelPerCrystal", 112, 1, 100000);
        ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL = BUILDER.comment("Seconds required to repair 1 full Observation Crystal worth of durability")
                .defineInRange("ancientTableRepairSecondsPerCrystal", 28, 1, 3600);
        ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM = BUILDER.comment("Seconds required per input item when crafting on the Ancient Table")
                .defineInRange("ancientTableCraftSecondsPerItem", 10, 1, 3600);
        BUILDER.pop();

        BUILDER.push("watchpoint");
        MAX_WATCH_POINTS_PER_PLAYER = BUILDER.comment("Max active Watch Point dummies per player")
                .defineInRange("maxWatchPointsPerPlayer", 5, 1, 100);
        MAX_WATCH_POINTS_TOTAL = BUILDER.comment("Max active Watch Point dummies across the entire server")
                .defineInRange("maxWatchPointsTotal", 50, 1, 1000);
        BUILDER.pop();

        BUILDER.push("podest");
        PODEST_BLOCKS_PER_CHARGE = BUILDER.comment("3D spherical protection radius in blocks per Observation Crystal charge (max 10 charges)")
                .defineInRange("podestBlocksPerCharge", 10.0D, 1.0D, 1000.0D);
        PODEST_JAMMER_NORMAL_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption when a Jammer (On mode) is active on a Pedestal")
                .defineInRange("podestJammerNormalIntervalTicks", 20, 1, 72000);
        PODEST_JAMMER_LIMITED_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption when a Jammer (Limited mode) is active on a Pedestal")
                .defineInRange("podestJammerLimitedIntervalTicks", 40, 1, 72000);
        PODEST_ANONYMIZER_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption when an Anonymizer is active on a Pedestal")
                .defineInRange("podestAnonymizerIntervalTicks", 20, 1, 72000);
        BUILDER.pop();

        BUILDER.push("dimension_blacklist");
        BLACKLISTED_DIMENSIONS = BUILDER.comment("List of dimension resource locations (e.g. [\"minecraft:the_end\"]) where Bodycam features are disabled")
                .defineListAllowEmpty(Collections.singletonList("blacklistedDimensions"), Collections::emptyList, o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("performance");
        PORTAL_SEARCH_CHUNK_RADIUS = BUILDER.comment("Max chunk radius to scan for active portal blocks in Player Locator")
                .defineInRange("portalSearchChunkRadius", 8, 1, 32);
        LOCATOR_SCAN_INTERVAL_TICKS = BUILDER.comment("Tick interval for Player Locator portal/structure scanning")
                .defineInRange("locatorScanIntervalTicks", 50, 1, 1200);
        PODEST_SPHERE_CHECK_INTERVAL_TICKS = BUILDER.comment("Tick interval for Podest redstone/state synchronization")
                .defineInRange("podestSphereCheckIntervalTicks", 1, 1, 200);
        JAMMER_NORMAL_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption for held/inventory Jammer (On mode)")
                .defineInRange("jammerNormalIntervalTicks", 20, 1, 72000);
        JAMMER_LIMITED_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption for held/inventory Jammer (Limited mode)")
                .defineInRange("jammerLimitedIntervalTicks", 40, 1, 72000);
        ANONYMIZER_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption for held/inventory Anonymizer")
                .defineInRange("anonymizerIntervalTicks", 20, 1, 72000);
        PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS = BUILDER.comment("Ticks between 1 durability consumption for active Player Locator")
                .defineInRange("playerLocatorDurabilityIntervalTicks", 20, 1, 72000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setActiveModConfig(ModConfig config) {
        activeModConfig = config;
    }

    public static boolean reloadFromDisk() {
        try {
            if (activeModConfig != null && activeModConfig.getConfigData() instanceof FileConfig fileConfig) {
                fileConfig.load();
                SPEC.afterReload();
                return true;
            }
            SPEC.afterReload();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDimensionBlacklisted(Level level) {
        if (level == null) {
            return false;
        }
        return isDimensionBlacklisted(level.dimension().location().toString());
    }

    public static boolean isDimensionBlacklisted(String dimensionId) {
        if (dimensionId == null || dimensionId.isEmpty() || !SPEC.isLoaded()) {
            return false;
        }
        for (String entry : BLACKLISTED_DIMENSIONS.get()) {
            if (dimensionId.equalsIgnoreCase(entry.trim())) {
                return true;
            }
        }
        return false;
    }
}
