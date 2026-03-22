package dev.ClasherHD.bodycam.config;

import net.minecraftforge.common.ForgeConfigSpec;

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
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
