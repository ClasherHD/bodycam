package dev.ClasherHD.bodycam.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_NAME_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue SHOW_HEALTH_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHIFT_OVERLAY;

    public static final ForgeConfigSpec.ConfigValue<String> COLOR_STANDARD;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_BLOCKED;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_OBSERVING;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_DIMENSION;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_OUT_OF_RANGE;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_OTHER_DIMENSION;

    private static ModConfig activeModConfig;

    static {
        BUILDER.push("UI Overlays");
        SHOW_NAME_OVERLAY = BUILDER.comment("Show the target's name while observing").define("showNameOverlay", true);
        SHOW_HEALTH_OVERLAY = BUILDER.comment("Show the target's health hearts while observing").define("showHealthOverlay", true);
        SHOW_SHIFT_OVERLAY = BUILDER.comment("Show the exit instruction while observing").define("showShiftOverlay", true);
        BUILDER.pop();

        BUILDER.push("GUI Colors");
        COLOR_STANDARD = BUILDER.comment("Hex color code for standard online player names").define("colorStandard", "FFFFFF");
        COLOR_BLOCKED = BUILDER.comment("Hex color code for jammed online player names").define("colorBlocked", "FF5555");
        COLOR_OBSERVING = BUILDER.comment("Hex color code for observing players").define("colorObserving", "5555FF");
        COLOR_DIMENSION = BUILDER.comment("Hex color code for dimensions in the Dimension Locator").define("colorDimension", "55FF55");
        COLOR_OUT_OF_RANGE = BUILDER.comment("Hex color code for out of range player names without reach").define("colorOutOfRange", "FFFF55");
        COLOR_OTHER_DIMENSION = BUILDER.comment("Hex color code for player names in another dimension while having Reach I").define("colorOtherDimension", "FFAA00");
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

    public static int parseHexColor(String hex, int fallback) {
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
