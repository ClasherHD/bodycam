package dev.ClasherHD.bodycam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;
import net.minecraft.world.level.Level;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean loaded = false;

    public static final class ConfigValue<T> implements Supplier<T> {
        private T value;
        private final T defaultValue;

        public ConfigValue(T defaultValue) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        @Override
        public T get() {
            return value;
        }

        public void set(T newValue) {
            this.value = newValue != null ? newValue : defaultValue;
        }

        public T getDefault() {
            return defaultValue;
        }
    }

    public static final ConfigValue<Boolean> OP_ONLY_MODE = new ConfigValue<>(false);
    public static final ConfigValue<Integer> MAX_MONITOR_DISTANCE = new ConfigValue<>(500);
    public static final ConfigValue<Boolean> ENABLE_REACH_ENCHANTMENT = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_JAMMER = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_DIMENSION_LOCATOR = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_HOLOGRAM_BLOCK = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_ANONYMIZER = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_PLAYER_LOCATOR = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_WATCH_POINT = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_PODEST = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_LOGGER = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_ALL_SEEING_EYE = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_CREEPING_DEATH = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_PORTABLE_NETHER_PORTAL = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_SCHOLAR_CURSE = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> ENABLE_CREEPING_DEATH_NUKE = new ConfigValue<>(true);

    public static final ConfigValue<Integer> SPYGLASS_DURABILITY = new ConfigValue<>(16);
    public static final ConfigValue<Integer> DIMENSION_LOCATOR_DURABILITY = new ConfigValue<>(8);
    public static final ConfigValue<Integer> PLAYER_LOCATOR_DURABILITY = new ConfigValue<>(560);
    public static final ConfigValue<Integer> ANONYMIZER_DURABILITY = new ConfigValue<>(448);
    public static final ConfigValue<Integer> JAMMER_DURABILITY = new ConfigValue<>(336);
    public static final ConfigValue<Integer> LOGGER_DURABILITY = new ConfigValue<>(8);
    public static final ConfigValue<Integer> ANCIENT_TABLE_FUEL_PER_CRYSTAL = new ConfigValue<>(112);
    public static final ConfigValue<Integer> ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL = new ConfigValue<>(28);
    public static final ConfigValue<Integer> ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM = new ConfigValue<>(10);

    public static final ConfigValue<Integer> MAX_WATCH_POINTS_PER_PLAYER = new ConfigValue<>(5);
    public static final ConfigValue<Integer> MAX_WATCH_POINTS_TOTAL = new ConfigValue<>(50);

    public static final ConfigValue<Double> PODEST_BLOCKS_PER_CHARGE = new ConfigValue<>(10.0D);
    public static final ConfigValue<Integer> PODEST_JAMMER_NORMAL_INTERVAL_TICKS = new ConfigValue<>(20);
    public static final ConfigValue<Integer> PODEST_JAMMER_LIMITED_INTERVAL_TICKS = new ConfigValue<>(40);
    public static final ConfigValue<Integer> PODEST_ANONYMIZER_INTERVAL_TICKS = new ConfigValue<>(20);

    public static final ConfigValue<List<String>> BLACKLISTED_DIMENSIONS = new ConfigValue<>(new ArrayList<>());

    public static final ConfigValue<Integer> PORTAL_SEARCH_CHUNK_RADIUS = new ConfigValue<>(8);
    public static final ConfigValue<Integer> LOCATOR_SCAN_INTERVAL_TICKS = new ConfigValue<>(50);
    public static final ConfigValue<Integer> PODEST_SPHERE_CHECK_INTERVAL_TICKS = new ConfigValue<>(1);
    public static final ConfigValue<Integer> JAMMER_NORMAL_INTERVAL_TICKS = new ConfigValue<>(20);
    public static final ConfigValue<Integer> JAMMER_LIMITED_INTERVAL_TICKS = new ConfigValue<>(40);
    public static final ConfigValue<Integer> ANONYMIZER_INTERVAL_TICKS = new ConfigValue<>(20);
    public static final ConfigValue<Integer> PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS = new ConfigValue<>(20);

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void load() {
        try {
            Path dir = BodycamPlatform.getConfigDirectory();
            Files.createDirectories(dir);
            Path file = dir.resolve("bodycam-server.json");
            if (!Files.exists(file)) {
                save();
                loaded = true;
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                if (obj == null) {
                    loaded = true;
                    return;
                }
                if (obj.has("opOnlyMode")) OP_ONLY_MODE.set(obj.get("opOnlyMode").getAsBoolean());
                if (obj.has("maxMonitorDistance")) MAX_MONITOR_DISTANCE.set(clampInt(obj.get("maxMonitorDistance").getAsInt(), 1, 1000000));
                if (obj.has("enableReachEnchantment")) ENABLE_REACH_ENCHANTMENT.set(obj.get("enableReachEnchantment").getAsBoolean());
                if (obj.has("enableJammer")) ENABLE_JAMMER.set(obj.get("enableJammer").getAsBoolean());
                if (obj.has("enableDimensionLocator")) ENABLE_DIMENSION_LOCATOR.set(obj.get("enableDimensionLocator").getAsBoolean());
                if (obj.has("enableHologramBlock")) ENABLE_HOLOGRAM_BLOCK.set(obj.get("enableHologramBlock").getAsBoolean());
                if (obj.has("enableAnonymizer")) ENABLE_ANONYMIZER.set(obj.get("enableAnonymizer").getAsBoolean());
                if (obj.has("enablePlayerLocator")) ENABLE_PLAYER_LOCATOR.set(obj.get("enablePlayerLocator").getAsBoolean());
                if (obj.has("enableWatchPoint")) ENABLE_WATCH_POINT.set(obj.get("enableWatchPoint").getAsBoolean());
                if (obj.has("enablePodest")) ENABLE_PODEST.set(obj.get("enablePodest").getAsBoolean());
                if (obj.has("enableLogger")) ENABLE_LOGGER.set(obj.get("enableLogger").getAsBoolean());
                if (obj.has("enableAllSeeingEye")) ENABLE_ALL_SEEING_EYE.set(obj.get("enableAllSeeingEye").getAsBoolean());
                if (obj.has("enableCreepingDeath")) ENABLE_CREEPING_DEATH.set(obj.get("enableCreepingDeath").getAsBoolean());
                if (obj.has("enablePortableNetherPortal")) ENABLE_PORTABLE_NETHER_PORTAL.set(obj.get("enablePortableNetherPortal").getAsBoolean());
                if (obj.has("enableScholarCurse")) ENABLE_SCHOLAR_CURSE.set(obj.get("enableScholarCurse").getAsBoolean());
                if (obj.has("enableCreepingDeathNuke")) ENABLE_CREEPING_DEATH_NUKE.set(obj.get("enableCreepingDeathNuke").getAsBoolean());

                if (obj.has("spyglassDurability")) SPYGLASS_DURABILITY.set(clampInt(obj.get("spyglassDurability").getAsInt(), 1, 100000));
                if (obj.has("dimensionLocatorDurability")) DIMENSION_LOCATOR_DURABILITY.set(clampInt(obj.get("dimensionLocatorDurability").getAsInt(), 1, 100000));
                if (obj.has("playerLocatorDurability")) PLAYER_LOCATOR_DURABILITY.set(clampInt(obj.get("playerLocatorDurability").getAsInt(), 1, 100000));
                if (obj.has("anonymizerDurability")) ANONYMIZER_DURABILITY.set(clampInt(obj.get("anonymizerDurability").getAsInt(), 1, 100000));
                if (obj.has("jammerDurability")) JAMMER_DURABILITY.set(clampInt(obj.get("jammerDurability").getAsInt(), 1, 100000));
                if (obj.has("loggerDurability")) LOGGER_DURABILITY.set(clampInt(obj.get("loggerDurability").getAsInt(), 1, 100000));
                if (obj.has("ancientTableFuelPerCrystal")) ANCIENT_TABLE_FUEL_PER_CRYSTAL.set(clampInt(obj.get("ancientTableFuelPerCrystal").getAsInt(), 1, 100000));
                if (obj.has("ancientTableRepairSecondsPerCrystal")) ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL.set(clampInt(obj.get("ancientTableRepairSecondsPerCrystal").getAsInt(), 1, 3600));
                if (obj.has("ancientTableCraftSecondsPerItem")) ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM.set(clampInt(obj.get("ancientTableCraftSecondsPerItem").getAsInt(), 1, 3600));

                if (obj.has("maxWatchPointsPerPlayer")) MAX_WATCH_POINTS_PER_PLAYER.set(clampInt(obj.get("maxWatchPointsPerPlayer").getAsInt(), 1, 100));
                if (obj.has("maxWatchPointsTotal")) MAX_WATCH_POINTS_TOTAL.set(clampInt(obj.get("maxWatchPointsTotal").getAsInt(), 1, 1000));

                if (obj.has("podestBlocksPerCharge")) PODEST_BLOCKS_PER_CHARGE.set(clampDouble(obj.get("podestBlocksPerCharge").getAsDouble(), 1.0D, 1000.0D));
                if (obj.has("podestJammerNormalIntervalTicks")) PODEST_JAMMER_NORMAL_INTERVAL_TICKS.set(clampInt(obj.get("podestJammerNormalIntervalTicks").getAsInt(), 1, 72000));
                if (obj.has("podestJammerLimitedIntervalTicks")) PODEST_JAMMER_LIMITED_INTERVAL_TICKS.set(clampInt(obj.get("podestJammerLimitedIntervalTicks").getAsInt(), 1, 72000));
                if (obj.has("podestAnonymizerIntervalTicks")) PODEST_ANONYMIZER_INTERVAL_TICKS.set(clampInt(obj.get("podestAnonymizerIntervalTicks").getAsInt(), 1, 72000));

                if (obj.has("blacklistedDimensions") && obj.get("blacklistedDimensions").isJsonArray()) {
                    List<String> dims = new ArrayList<>();
                    for (JsonElement elem : obj.getAsJsonArray("blacklistedDimensions")) {
                        if (elem != null && elem.isJsonPrimitive()) {
                            String entry = elem.getAsString().trim();
                            if (!entry.isEmpty()) {
                                dims.add(entry);
                            }
                        }
                    }
                    BLACKLISTED_DIMENSIONS.set(dims);
                } else {
                    BLACKLISTED_DIMENSIONS.set(new ArrayList<>());
                }

                if (obj.has("portalSearchChunkRadius")) PORTAL_SEARCH_CHUNK_RADIUS.set(clampInt(obj.get("portalSearchChunkRadius").getAsInt(), 1, 32));
                if (obj.has("locatorScanIntervalTicks")) LOCATOR_SCAN_INTERVAL_TICKS.set(clampInt(obj.get("locatorScanIntervalTicks").getAsInt(), 1, 1200));
                if (obj.has("podestSphereCheckIntervalTicks")) PODEST_SPHERE_CHECK_INTERVAL_TICKS.set(clampInt(obj.get("podestSphereCheckIntervalTicks").getAsInt(), 1, 200));
                if (obj.has("jammerNormalIntervalTicks")) JAMMER_NORMAL_INTERVAL_TICKS.set(clampInt(obj.get("jammerNormalIntervalTicks").getAsInt(), 1, 72000));
                if (obj.has("jammerLimitedIntervalTicks")) JAMMER_LIMITED_INTERVAL_TICKS.set(clampInt(obj.get("jammerLimitedIntervalTicks").getAsInt(), 1, 72000));
                if (obj.has("anonymizerIntervalTicks")) ANONYMIZER_INTERVAL_TICKS.set(clampInt(obj.get("anonymizerIntervalTicks").getAsInt(), 1, 72000));
                if (obj.has("playerLocatorDurabilityIntervalTicks")) PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS.set(clampInt(obj.get("playerLocatorDurabilityIntervalTicks").getAsInt(), 1, 72000));
            }
            loaded = true;
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Path dir = BodycamPlatform.getConfigDirectory();
            Files.createDirectories(dir);
            Path file = dir.resolve("bodycam-server.json");
            JsonObject obj = new JsonObject();
            obj.addProperty("opOnlyMode", OP_ONLY_MODE.get());
            obj.addProperty("maxMonitorDistance", clampInt(MAX_MONITOR_DISTANCE.get(), 1, 1000000));
            obj.addProperty("enableReachEnchantment", ENABLE_REACH_ENCHANTMENT.get());
            obj.addProperty("enableJammer", ENABLE_JAMMER.get());
            obj.addProperty("enableDimensionLocator", ENABLE_DIMENSION_LOCATOR.get());
            obj.addProperty("enableHologramBlock", ENABLE_HOLOGRAM_BLOCK.get());
            obj.addProperty("enableAnonymizer", ENABLE_ANONYMIZER.get());
            obj.addProperty("enablePlayerLocator", ENABLE_PLAYER_LOCATOR.get());
            obj.addProperty("enableWatchPoint", ENABLE_WATCH_POINT.get());
            obj.addProperty("enablePodest", ENABLE_PODEST.get());
            obj.addProperty("enableLogger", ENABLE_LOGGER.get());
            obj.addProperty("enableAllSeeingEye", ENABLE_ALL_SEEING_EYE.get());
            obj.addProperty("enableCreepingDeath", ENABLE_CREEPING_DEATH.get());
            obj.addProperty("enablePortableNetherPortal", ENABLE_PORTABLE_NETHER_PORTAL.get());
            obj.addProperty("enableScholarCurse", ENABLE_SCHOLAR_CURSE.get());
            obj.addProperty("enableCreepingDeathNuke", ENABLE_CREEPING_DEATH_NUKE.get());

            obj.addProperty("spyglassDurability", clampInt(SPYGLASS_DURABILITY.get(), 1, 100000));
            obj.addProperty("dimensionLocatorDurability", clampInt(DIMENSION_LOCATOR_DURABILITY.get(), 1, 100000));
            obj.addProperty("playerLocatorDurability", clampInt(PLAYER_LOCATOR_DURABILITY.get(), 1, 100000));
            obj.addProperty("anonymizerDurability", clampInt(ANONYMIZER_DURABILITY.get(), 1, 100000));
            obj.addProperty("jammerDurability", clampInt(JAMMER_DURABILITY.get(), 1, 100000));
            obj.addProperty("loggerDurability", clampInt(LOGGER_DURABILITY.get(), 1, 100000));
            obj.addProperty("ancientTableFuelPerCrystal", clampInt(ANCIENT_TABLE_FUEL_PER_CRYSTAL.get(), 1, 100000));
            obj.addProperty("ancientTableRepairSecondsPerCrystal", clampInt(ANCIENT_TABLE_REPAIR_SECONDS_PER_CRYSTAL.get(), 1, 3600));
            obj.addProperty("ancientTableCraftSecondsPerItem", clampInt(ANCIENT_TABLE_CRAFT_SECONDS_PER_ITEM.get(), 1, 3600));

            obj.addProperty("maxWatchPointsPerPlayer", clampInt(MAX_WATCH_POINTS_PER_PLAYER.get(), 1, 100));
            obj.addProperty("maxWatchPointsTotal", clampInt(MAX_WATCH_POINTS_TOTAL.get(), 1, 1000));

            obj.addProperty("podestBlocksPerCharge", clampDouble(PODEST_BLOCKS_PER_CHARGE.get(), 1.0D, 1000.0D));
            obj.addProperty("podestJammerNormalIntervalTicks", clampInt(PODEST_JAMMER_NORMAL_INTERVAL_TICKS.get(), 1, 72000));
            obj.addProperty("podestJammerLimitedIntervalTicks", clampInt(PODEST_JAMMER_LIMITED_INTERVAL_TICKS.get(), 1, 72000));
            obj.addProperty("podestAnonymizerIntervalTicks", clampInt(PODEST_ANONYMIZER_INTERVAL_TICKS.get(), 1, 72000));

            JsonArray dimsArray = new JsonArray();
            List<String> dims = BLACKLISTED_DIMENSIONS.get();
            if (dims != null) {
                for (String entry : dims) {
                    if (entry != null && !entry.trim().isEmpty()) {
                        dimsArray.add(entry.trim());
                    }
                }
            }
            obj.add("blacklistedDimensions", dimsArray);

            obj.addProperty("portalSearchChunkRadius", clampInt(PORTAL_SEARCH_CHUNK_RADIUS.get(), 1, 32));
            obj.addProperty("locatorScanIntervalTicks", clampInt(LOCATOR_SCAN_INTERVAL_TICKS.get(), 1, 1200));
            obj.addProperty("podestSphereCheckIntervalTicks", clampInt(PODEST_SPHERE_CHECK_INTERVAL_TICKS.get(), 1, 200));
            obj.addProperty("jammerNormalIntervalTicks", clampInt(JAMMER_NORMAL_INTERVAL_TICKS.get(), 1, 72000));
            obj.addProperty("jammerLimitedIntervalTicks", clampInt(JAMMER_LIMITED_INTERVAL_TICKS.get(), 1, 72000));
            obj.addProperty("anonymizerIntervalTicks", clampInt(ANONYMIZER_INTERVAL_TICKS.get(), 1, 72000));
            obj.addProperty("playerLocatorDurabilityIntervalTicks", clampInt(PLAYER_LOCATOR_DURABILITY_INTERVAL_TICKS.get(), 1, 72000));

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(obj, writer);
            }
            loaded = true;
        } catch (Exception ignored) {
        }
    }

    public static boolean reloadFromDisk() {
        try {
            load();
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
        if (dimensionId == null || dimensionId.isEmpty()) {
            return false;
        }
        List<String> list = BLACKLISTED_DIMENSIONS.get();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (String entry : list) {
            if (entry != null && dimensionId.equalsIgnoreCase(entry.trim())) {
                return true;
            }
        }
        return false;
    }
}
