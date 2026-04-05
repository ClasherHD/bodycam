package dev.ClasherHD.bodycam.config;

public class ModClientConfig {
    public static final ConfigValue<Boolean> SHOW_NAME_OVERLAY = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> SHOW_HEALTH_OVERLAY = new ConfigValue<>(true);
    public static final ConfigValue<Boolean> SHOW_SHIFT_OVERLAY = new ConfigValue<>(true);

    public static final ConfigValue<String> COLOR_STANDARD = new ConfigValue<>("FFFFFF");
    public static final ConfigValue<String> COLOR_BLOCKED = new ConfigValue<>("FF5555");
    public static final ConfigValue<String> COLOR_OBSERVING = new ConfigValue<>("5555FF");
    public static final ConfigValue<String> COLOR_DIMENSION = new ConfigValue<>("55FF55");

    public static void load() {
        java.io.File file = new java.io.File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(),
                "bodycam-client.json");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try {
            if (file.exists()) {
                com.google.gson.JsonObject json = gson.fromJson(new java.io.FileReader(file),
                        com.google.gson.JsonObject.class);
                if (json.has("showNameOverlay"))
                    SHOW_NAME_OVERLAY.set(json.get("showNameOverlay").getAsBoolean());
                if (json.has("showHealthOverlay"))
                    SHOW_HEALTH_OVERLAY.set(json.get("showHealthOverlay").getAsBoolean());
                if (json.has("showShiftOverlay"))
                    SHOW_SHIFT_OVERLAY.set(json.get("showShiftOverlay").getAsBoolean());
                if (json.has("colorStandard"))
                    COLOR_STANDARD.set(json.get("colorStandard").getAsString());
                if (json.has("colorBlocked"))
                    COLOR_BLOCKED.set(json.get("colorBlocked").getAsString());
                if (json.has("colorObserving"))
                    COLOR_OBSERVING.set(json.get("colorObserving").getAsString());
                if (json.has("colorDimension"))
                    COLOR_DIMENSION.set(json.get("colorDimension").getAsString());
            } else {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("showNameOverlay", SHOW_NAME_OVERLAY.get());
                obj.addProperty("showHealthOverlay", SHOW_HEALTH_OVERLAY.get());
                obj.addProperty("showShiftOverlay", SHOW_SHIFT_OVERLAY.get());
                obj.addProperty("colorStandard", COLOR_STANDARD.get());
                obj.addProperty("colorBlocked", COLOR_BLOCKED.get());
                obj.addProperty("colorObserving", COLOR_OBSERVING.get());
                obj.addProperty("colorDimension", COLOR_DIMENSION.get());
                java.io.FileWriter writer = new java.io.FileWriter(file);
                gson.toJson(obj, writer);
                writer.close();
            }
        } catch (Exception e) {
        }
    }

    public static void save() {
        java.io.File file = new java.io.File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(),
                "bodycam-client.json");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        try {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("showNameOverlay", SHOW_NAME_OVERLAY.get());
            obj.addProperty("showHealthOverlay", SHOW_HEALTH_OVERLAY.get());
            obj.addProperty("showShiftOverlay", SHOW_SHIFT_OVERLAY.get());
            obj.addProperty("colorStandard", COLOR_STANDARD.get());
            obj.addProperty("colorBlocked", COLOR_BLOCKED.get());
            obj.addProperty("colorObserving", COLOR_OBSERVING.get());
            obj.addProperty("colorDimension", COLOR_DIMENSION.get());
            java.io.FileWriter writer = new java.io.FileWriter(file);
            gson.toJson(obj, writer);
            writer.close();
        } catch (Exception e) {
        }
    }

    public static class ConfigValue<T> {
        private T value;

        public ConfigValue(T def) {
            this.value = def;
        }

        public T get() {
            return value;
        }

        public void set(T val) {
            this.value = val;
        }
    }
}
