package dev.ClasherHD.bodycam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.ClasherHD.bodycam.config.ModServerConfig.ConfigValue;
import dev.ClasherHD.bodycam.platform.BodycamPlatform;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ConfigValue<Boolean> SHOW_NAME_OVERLAY = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> SHOW_HEALTH_OVERLAY = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> SHOW_SHIFT_OVERLAY = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> SHOW_DAMAGE_VIGNETTE = new ConfigValue<>(true);

    public static final ConfigValue<String> COLOR_STANDARD = new ConfigValue<>("FFFFFF");
    public static final ConfigValue<String> COLOR_BLOCKED = new ConfigValue<>("FF5555");
    public static final ConfigValue<String> COLOR_OBSERVING = new ConfigValue<>("5555FF");
    public static final ConfigValue<String> COLOR_DIMENSION = new ConfigValue<>("55FF55");
    public static final ConfigValue<String> COLOR_OUT_OF_RANGE = new ConfigValue<>("FFFF55");
    public static final ConfigValue<String> COLOR_OTHER_DIMENSION = new ConfigValue<>("FFAA00");

    public static void load() {
        try {
            Path dir = BodycamPlatform.getConfigDirectory();
            Files.createDirectories(dir);
            Path file = dir.resolve("bodycam-client.json");
            if (!Files.exists(file)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                if (obj == null) {
                    return;
                }
                if (obj.has("showNameOverlay")) SHOW_NAME_OVERLAY.set(obj.get("showNameOverlay").getAsBoolean());
                if (obj.has("showHealthOverlay")) SHOW_HEALTH_OVERLAY.set(obj.get("showHealthOverlay").getAsBoolean());
                if (obj.has("showShiftOverlay")) SHOW_SHIFT_OVERLAY.set(obj.get("showShiftOverlay").getAsBoolean());
                if (obj.has("showDamageVignette")) SHOW_DAMAGE_VIGNETTE.set(obj.get("showDamageVignette").getAsBoolean());
                if (obj.has("colorStandard")) COLOR_STANDARD.set(obj.get("colorStandard").getAsString());
                if (obj.has("colorBlocked")) COLOR_BLOCKED.set(obj.get("colorBlocked").getAsString());
                if (obj.has("colorObserving")) COLOR_OBSERVING.set(obj.get("colorObserving").getAsString());
                if (obj.has("colorDimension")) COLOR_DIMENSION.set(obj.get("colorDimension").getAsString());
                if (obj.has("colorOutOfRange")) COLOR_OUT_OF_RANGE.set(obj.get("colorOutOfRange").getAsString());
                if (obj.has("colorOtherDimension")) COLOR_OTHER_DIMENSION.set(obj.get("colorOtherDimension").getAsString());
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Path dir = BodycamPlatform.getConfigDirectory();
            Files.createDirectories(dir);
            Path file = dir.resolve("bodycam-client.json");
            JsonObject obj = new JsonObject();
            obj.addProperty("showNameOverlay", SHOW_NAME_OVERLAY.get());
            obj.addProperty("showHealthOverlay", SHOW_HEALTH_OVERLAY.get());
            obj.addProperty("showShiftOverlay", SHOW_SHIFT_OVERLAY.get());
            obj.addProperty("showDamageVignette", SHOW_DAMAGE_VIGNETTE.get());
            obj.addProperty("colorStandard", COLOR_STANDARD.get());
            obj.addProperty("colorBlocked", COLOR_BLOCKED.get());
            obj.addProperty("colorObserving", COLOR_OBSERVING.get());
            obj.addProperty("colorDimension", COLOR_DIMENSION.get());
            obj.addProperty("colorOutOfRange", COLOR_OUT_OF_RANGE.get());
            obj.addProperty("colorOtherDimension", COLOR_OTHER_DIMENSION.get());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(obj, writer);
            }
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

    public static int parseHexColor(String hex, int fallback) {
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        String cleaned = hex.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        } else if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
